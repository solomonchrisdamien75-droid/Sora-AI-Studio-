package com.example.ai.server

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.example.ai.inference.InferenceEngineManager
import com.example.data.AiModelEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class LocalApiServer(
    private val context: Context,
    private val engineManager: InferenceEngineManager
) {
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private var serverJob: Job? = null
    private var uptimeTimerJob: Job? = null

    private val requestCount = AtomicLong(0L)
    private val tokensGenerated = AtomicLong(0L)
    private var startTimestamp = 0L

    private val _serverState = MutableStateFlow(ServerState())
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    init {
        // Observe active model changes from engineManager and update server state dynamically
        serverScope.launch {
            engineManager.activeLoadedModel.collect { model ->
                val backendInfo = engineManager.getBackendInfoForModel(model)
                _serverState.value = _serverState.value.copy(
                    activeModel = model,
                    backendInfo = backendInfo
                )
            }
        }
    }

    fun updateConfig(config: ServerConfig) {
        _serverState.value = _serverState.value.copy(
            config = config,
            port = config.port,
            localUrl = "http://127.0.0.1:${config.port}/v1",
            networkUrl = "http://${getDeviceIpAddress()}:${config.port}/v1",
            tunnelUrl = if (config.tunnelEnabled) "https://${config.tunnelSubdomain}.trycloudflare.com/v1" else null
        )
    }

    @Synchronized
    fun startServer(): Pair<Boolean, String> {
        if (isRunning.get()) {
            return Pair(true, "API Server is already running on port ${_serverState.value.port}")
        }

        val currentModel = engineManager.activeLoadedModel.value
        val backendInfo = engineManager.getBackendInfoForModel(currentModel)

        if (currentModel == null || !backendInfo.isServerCompatible) {
            return Pair(false, "Cannot start server: No compatible model loaded. Please load a GGUF, LiteRT, or ONNX model.")
        }

        val port = _serverState.value.config.port
        try {
            serverSocket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
            serverSocket?.reuseAddress = true
            isRunning.set(true)
            startTimestamp = System.currentTimeMillis()

            val deviceIp = getDeviceIpAddress()
            val tunnelUrl = if (_serverState.value.config.tunnelEnabled) {
                "https://${_serverState.value.config.tunnelSubdomain}.trycloudflare.com/v1"
            } else null

            _serverState.value = _serverState.value.copy(
                status = ServerStatus.RUNNING,
                port = port,
                localUrl = "http://127.0.0.1:$port/v1",
                networkUrl = "http://$deviceIp:$port/v1",
                tunnelUrl = tunnelUrl,
                uptimeSeconds = 0L,
                errorMessage = null
            )

            // Start uptime counter
            uptimeTimerJob?.cancel()
            uptimeTimerJob = serverScope.launch {
                while (isRunning.get()) {
                    delay(1000)
                    val uptime = (System.currentTimeMillis() - startTimestamp) / 1000
                    _serverState.value = _serverState.value.copy(uptimeSeconds = uptime)
                }
            }

            // Start client accept loop
            serverJob = serverScope.launch {
                while (isRunning.get() && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        launch {
                            handleClientSocket(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            // Socket error while active
                        }
                    }
                }
            }

            return Pair(true, "API Server running on port $port serving '${currentModel.name}' (${backendInfo.backend})")
        } catch (e: Exception) {
            isRunning.set(false)
            _serverState.value = _serverState.value.copy(
                status = ServerStatus.ERROR,
                errorMessage = "Failed to start server on port $port: ${e.localizedMessage}"
            )
            return Pair(false, "Failed to start server on port $port: ${e.localizedMessage}")
        }
    }

    @Synchronized
    fun stopServer() {
        if (!isRunning.get()) return

        isRunning.set(false)
        uptimeTimerJob?.cancel()
        serverJob?.cancel()

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        _serverState.value = _serverState.value.copy(
            status = ServerStatus.STOPPED,
            uptimeSeconds = 0L
        )
    }

    private suspend fun handleClientSocket(socket: Socket) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            socket.soTimeout = 45000 // 45 sec socket timeout
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = BufferedOutputStream(socket.getOutputStream())

            // Read HTTP request line
            val requestLine = input.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0].uppercase()
            val fullPath = parts[1]
            val path = fullPath.substringBefore("?")

            // Read HTTP headers
            val headers = mutableMapOf<String, String>()
            var headerLine: String?
            var contentLength = 0
            while (input.readLine().also { headerLine = it } != null && headerLine!!.isNotBlank()) {
                val colonIdx = headerLine!!.indexOf(':')
                if (colonIdx > 0) {
                    val key = headerLine!!.substring(0, colonIdx).trim().lowercase()
                    val value = headerLine!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
            }

            // Read Body if present
            val bodyBuilder = StringBuilder()
            if (contentLength > 0) {
                val buffer = CharArray(1024)
                var bytesReadTotal = 0
                while (bytesReadTotal < contentLength) {
                    val toRead = minOf(buffer.size, contentLength - bytesReadTotal)
                    val read = input.read(buffer, 0, toRead)
                    if (read == -1) break
                    bodyBuilder.append(buffer, 0, read)
                    bytesReadTotal += read
                }
            }
            val requestBody = bodyBuilder.toString()

            // CORS Preflight
            if (method == "OPTIONS") {
                sendCorsResponse(output)
                return@withContext
            }

            // API Key Authentication check
            val config = _serverState.value.config
            if (config.apiKeyEnabled) {
                val authHeader = headers["authorization"] ?: ""
                val bearerToken = if (authHeader.startsWith("Bearer ", ignoreCase = true)) {
                    authHeader.substring(7).trim()
                } else ""

                if (bearerToken != config.apiKey) {
                    sendErrorResponse(
                        output = output,
                        httpCode = 401,
                        message = "Incorrect API key provided. Check your server settings in the Sora AI Studio app.",
                        type = "invalid_request_error",
                        code = "invalid_api_key"
                    )
                    return@withContext
                }
            }

            // Update Metrics
            val totalReqs = requestCount.incrementAndGet()
            _serverState.value = _serverState.value.copy(
                requestCount = totalReqs,
                lastRequestTimestamp = System.currentTimeMillis(),
                lastRequestPath = "$method $path"
            )

            // Route Handler
            when {
                // Models listing
                (method == "GET" && (path == "/v1/models" || path == "/models")) -> {
                    handleGetModels(output)
                }

                // Chat Completions
                (method == "POST" && (path == "/v1/chat/completions" || path == "/chat/completions")) -> {
                    handleChatCompletions(requestBody, output)
                }

                // Standard Completions
                (method == "POST" && (path == "/v1/completions" || path == "/completions")) -> {
                    handleTextCompletions(requestBody, output)
                }

                // Embeddings
                (method == "POST" && (path == "/v1/embeddings" || path == "/embeddings")) -> {
                    handleEmbeddings(requestBody, output)
                }

                // Health check & Server info
                (method == "GET" && (path == "/" || path == "/v1" || path == "/health" || path == "/v1/health")) -> {
                    handleHealthCheck(output)
                }

                else -> {
                    sendErrorResponse(
                        output = output,
                        httpCode = 404,
                        message = "Unknown endpoint: $method $path. Supported endpoints: GET /v1/models, POST /v1/chat/completions, POST /v1/completions, POST /v1/embeddings",
                        type = "invalid_request_error",
                        code = "not_found"
                    )
                }
            }

            val latency = System.currentTimeMillis() - startTime
            _serverState.value = _serverState.value.copy(lastLatencyMs = latency)

        } catch (e: Exception) {
            try {
                sendErrorResponse(
                    output = BufferedOutputStream(socket.getOutputStream()),
                    httpCode = 500,
                    message = "Internal server error: ${e.localizedMessage}",
                    type = "api_error",
                    code = "internal_error"
                )
            } catch (_: Exception) {}
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private suspend fun handleGetModels(output: OutputStream) {
        val currentModel = engineManager.activeLoadedModel.value
        val engine = engineManager.activeEngine.value

        val modelsArray = JSONArray()
        if (currentModel != null) {
            val modelObj = JSONObject().apply {
                put("id", currentModel.name.replace(" ", "-"))
                put("object", "model")
                put("created", currentModel.dateAdded / 1000)
                put("owned_by", "local")
                put("format", currentModel.format)
                put("backend", engine?.backendType ?: "local")
                put("permission", JSONArray())
                put("root", currentModel.name)
                put("parent", JSONObject.NULL)
            }
            modelsArray.put(modelObj)
        }

        val responseJson = JSONObject().apply {
            put("object", "list")
            put("data", modelsArray)
        }

        sendJsonResponse(output, 200, responseJson.toString())
    }

    private suspend fun handleChatCompletions(body: String, output: OutputStream) {
        val currentModel = engineManager.activeLoadedModel.value
        val engine = engineManager.activeEngine.value

        if (currentModel == null || engine == null) {
            sendErrorResponse(
                output = output,
                httpCode = 503,
                message = "No model currently loaded on device. Load a model in the Server or Model Manager screen before sending requests.",
                type = "server_error",
                code = "no_model_loaded"
            )
            return
        }

        val json = try {
            JSONObject(body.ifBlank { "{}" })
        } catch (e: Exception) {
            sendErrorResponse(output, 400, "Malformed JSON request body: ${e.message}", "invalid_request_error", "bad_request")
            return
        }

        val stream = json.optBoolean("stream", false)
        val temperature = json.optDouble("temperature", 0.7).toFloat()
        val maxTokens = json.optInt("max_tokens", 512)

        val messagesJson = json.optJSONArray("messages") ?: JSONArray()
        val promptBuilder = StringBuilder()

        for (i in 0 until messagesJson.length()) {
            val msg = messagesJson.optJSONObject(i) ?: continue
            val role = msg.optString("role", "user")
            val content = msg.optString("content", "")
            when (role.lowercase()) {
                "system" -> promptBuilder.append("System Instructions: $content\n")
                "assistant" -> promptBuilder.append("Assistant: $content\n")
                else -> promptBuilder.append("User: $content\n")
            }
        }

        val prompt = promptBuilder.toString().ifBlank { json.optString("prompt", "Hello") }
        val completionId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").take(16)
        val createdTimestamp = System.currentTimeMillis() / 1000

        if (stream) {
            if (!engine.supportsStreaming()) {
                sendErrorResponse(
                    output = output,
                    httpCode = 400,
                    message = "Streaming is not supported by current inference backend (${engine.backendType}). Set 'stream': false.",
                    type = "invalid_request_error",
                    code = "streaming_not_supported"
                )
                return
            }

            // Stream response using SSE
            sendSseHeader(output)
            val outWriter = OutputStreamWriter(output, Charsets.UTF_8)
            var tokenCount = 0

            try {
                engine.streamText(prompt, maxTokens, temperature).collect { tokenChunk ->
                    tokenCount++
                    tokensGenerated.incrementAndGet()

                    val chunkJson = JSONObject().apply {
                        put("id", completionId)
                        put("object", "chat.completion.chunk")
                        put("created", createdTimestamp)
                        put("model", currentModel.name)
                        put("choices", JSONArray().apply {
                            put(JSONObject().apply {
                                put("index", 0)
                                put("delta", JSONObject().apply {
                                    put("content", tokenChunk)
                                })
                                put("finish_reason", JSONObject.NULL)
                            })
                        })
                    }

                    outWriter.write("data: ${chunkJson}\n\n")
                    outWriter.flush()
                }

                // Send final [DONE] chunk
                val finalChunk = JSONObject().apply {
                    put("id", completionId)
                    put("object", "chat.completion.chunk")
                    put("created", createdTimestamp)
                    put("model", currentModel.name)
                    put("choices", JSONArray().apply {
                        put(JSONObject().apply {
                            put("index", 0)
                            put("delta", JSONObject())
                            put("finish_reason", "stop")
                        })
                    })
                }
                outWriter.write("data: ${finalChunk}\n\n")
                outWriter.write("data: [DONE]\n\n")
                outWriter.flush()

                _serverState.value = _serverState.value.copy(tokensGenerated = tokensGenerated.get())
            } catch (e: Exception) {
                // Client disconnected
            }
        } else {
            // Non-streaming response
            val responseText = engineManager.runExclusiveInference { activeEngine ->
                activeEngine.generateText(prompt, maxTokens, temperature)
            }

            val promptTokens = maxOf(1, prompt.split(" ").size)
            val completionTokens = maxOf(1, responseText.split(" ").size)
            tokensGenerated.addAndGet(completionTokens.toLong())
            _serverState.value = _serverState.value.copy(tokensGenerated = tokensGenerated.get())

            val responseJson = JSONObject().apply {
                put("id", completionId)
                put("object", "chat.completion")
                put("created", createdTimestamp)
                put("model", currentModel.name)
                put("choices", JSONArray().apply {
                    put(JSONObject().apply {
                        put("index", 0)
                        put("message", JSONObject().apply {
                            put("role", "assistant")
                            put("content", responseText)
                        })
                        put("finish_reason", "stop")
                    })
                })
                put("usage", JSONObject().apply {
                    put("prompt_tokens", promptTokens)
                    put("completion_tokens", completionTokens)
                    put("total_tokens", promptTokens + completionTokens)
                })
            }

            sendJsonResponse(output, 200, responseJson.toString())
        }
    }

    private suspend fun handleTextCompletions(body: String, output: OutputStream) {
        val currentModel = engineManager.activeLoadedModel.value
        val engine = engineManager.activeEngine.value

        if (currentModel == null || engine == null) {
            sendErrorResponse(output, 503, "No model loaded on device.", "server_error", "no_model_loaded")
            return
        }

        val json = try {
            JSONObject(body.ifBlank { "{}" })
        } catch (e: Exception) {
            sendErrorResponse(output, 400, "Malformed JSON", "invalid_request_error", "bad_request")
            return
        }

        val prompt = json.optString("prompt", "Hello")
        val maxTokens = json.optInt("max_tokens", 256)
        val temperature = json.optDouble("temperature", 0.7).toFloat()
        val completionId = "cmpl-" + UUID.randomUUID().toString().replace("-", "").take(16)
        val createdTimestamp = System.currentTimeMillis() / 1000

        val responseText = engineManager.runExclusiveInference { activeEngine ->
            activeEngine.generateText(prompt, maxTokens, temperature)
        }

        val promptTokens = maxOf(1, prompt.split(" ").size)
        val completionTokens = maxOf(1, responseText.split(" ").size)
        tokensGenerated.addAndGet(completionTokens.toLong())

        val responseJson = JSONObject().apply {
            put("id", completionId)
            put("object", "text_completion")
            put("created", createdTimestamp)
            put("model", currentModel.name)
            put("choices", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", responseText)
                    put("index", 0)
                    put("finish_reason", "stop")
                })
            })
            put("usage", JSONObject().apply {
                put("prompt_tokens", promptTokens)
                put("completion_tokens", completionTokens)
                put("total_tokens", promptTokens + completionTokens)
            })
        }

        sendJsonResponse(output, 200, responseJson.toString())
    }

    private suspend fun handleEmbeddings(body: String, output: OutputStream) {
        val currentModel = engineManager.activeLoadedModel.value
        val engine = engineManager.activeEngine.value

        if (currentModel == null || engine == null) {
            sendErrorResponse(output, 503, "No model loaded on device.", "server_error", "no_model_loaded")
            return
        }

        val json = try {
            JSONObject(body.ifBlank { "{}" })
        } catch (e: Exception) {
            sendErrorResponse(output, 400, "Malformed JSON", "invalid_request_error", "bad_request")
            return
        }

        val input = json.optString("input", "Text embedding input")
        val embeddings = engine.generateEmbeddings(input)

        val embeddingArray = JSONArray()
        embeddings.forEach { embeddingArray.put(it.toDouble()) }

        val dataArray = JSONArray().apply {
            put(JSONObject().apply {
                put("object", "embedding")
                put("index", 0)
                put("embedding", embeddingArray)
            })
        }

        val responseJson = JSONObject().apply {
            put("object", "list")
            put("data", dataArray)
            put("model", currentModel.name)
            put("usage", JSONObject().apply {
                put("prompt_tokens", input.split(" ").size)
                put("total_tokens", input.split(" ").size)
            })
        }

        sendJsonResponse(output, 200, responseJson.toString())
    }

    private fun handleHealthCheck(output: OutputStream) {
        val currentModel = engineManager.activeLoadedModel.value
        val engine = engineManager.activeEngine.value

        val responseJson = JSONObject().apply {
            put("status", "healthy")
            put("service", "Sora Universal Model Server")
            put("version", "1.0.0")
            put("uptime_seconds", _serverState.value.uptimeSeconds)
            put("loaded_model", currentModel?.name ?: "None")
            put("format", currentModel?.format ?: "None")
            put("backend", engine?.backendType ?: "None")
            put("endpoints", JSONArray().apply {
                put("GET  /v1/models")
                put("POST /v1/chat/completions")
                put("POST /v1/completions")
                put("POST /v1/embeddings")
                put("GET  /health")
            })
        }

        sendJsonResponse(output, 200, responseJson.toString())
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val statusText = when (statusCode) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            503 -> "Service Unavailable"
            else -> "Internal Server Error"
        }
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS, PUT, DELETE\r\n" +
                "Access-Control-Allow-Headers: Authorization, Content-Type, Accept\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendSseHeader(output: OutputStream) {
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/event-stream; charset=utf-8\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Authorization, Content-Type, Accept\r\n" +
                "Connection: keep-alive\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun sendCorsResponse(output: OutputStream) {
        val header = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS, PUT, DELETE\r\n" +
                "Access-Control-Allow-Headers: Authorization, Content-Type, Accept\r\n" +
                "Access-Control-Max-Age: 86400\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun sendErrorResponse(
        output: OutputStream,
        httpCode: Int,
        message: String,
        type: String,
        code: String
    ) {
        val errObj = JSONObject().apply {
            put("error", JSONObject().apply {
                put("message", message)
                put("type", type)
                put("param", JSONObject.NULL)
                put("code", code)
            })
        }
        sendJsonResponse(output, httpCode, errObj.toString())
    }

    fun getDeviceIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotBlank() && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return Formatter.formatIpAddress(ipInt)
            }
        } catch (_: Exception) {}

        return "127.0.0.1"
    }
}
