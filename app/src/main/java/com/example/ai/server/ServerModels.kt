package com.example.ai.server

import com.example.data.AiModelEntity

enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

data class ServerModelBackendInfo(
    val modelName: String,
    val format: String,
    val backend: String,
    val isServerCompatible: Boolean,
    val supportsStreaming: Boolean,
    val supportsEmbeddings: Boolean,
    val statusMessage: String
)

data class ServerConfig(
    val port: Int = 8080,
    val apiKeyEnabled: Boolean = true,
    val apiKey: String = generateOpenAiApiKey("default"),
    val tunnelEnabled: Boolean = false,
    val tunnelSubdomain: String = "sora-local-ai",
    val maxConcurrentRequests: Int = 4,
    val timeoutSeconds: Int = 60
)

fun generateOpenAiApiKey(modelName: String): String {
    val cleanName = modelName.lowercase()
        .replace(Regex("[^a-z0-9]"), "-")
        .trim('-')
        .take(16)
        .ifBlank { "local" }
    val randomPart = java.util.UUID.randomUUID().toString().replace("-", "").take(24)
    return "sk-proj-$cleanName-$randomPart"
}

data class ServerState(
    val status: ServerStatus = ServerStatus.STOPPED,
    val port: Int = 8080,
    val localUrl: String = "http://127.0.0.1:8080/v1",
    val networkUrl: String = "http://127.0.0.1:8080/v1",
    val tunnelUrl: String? = null,
    val config: ServerConfig = ServerConfig(),
    val activeModel: AiModelEntity? = null,
    val generatedModelApiKey: String = generateOpenAiApiKey("default"),
    val backendInfo: ServerModelBackendInfo? = null,
    val requestCount: Long = 0L,
    val tokensGenerated: Long = 0L,
    val uptimeSeconds: Long = 0L,
    val lastRequestTimestamp: Long? = null,
    val lastRequestPath: String? = null,
    val lastLatencyMs: Long = 0L,
    val errorMessage: String? = null
)

data class OpenAiChatMessage(
    val role: String = "user",
    val content: String = ""
)

data class OpenAiChatCompletionRequest(
    val model: String = "",
    val messages: List<OpenAiChatMessage> = emptyList(),
    val temperature: Float = 0.7f,
    val max_tokens: Int = 512,
    val stream: Boolean = false
)
