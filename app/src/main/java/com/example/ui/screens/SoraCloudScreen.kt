package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.server.ServerStatus
import com.example.data.AiModelEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SoraCloudScreen(viewModel: SoraMainViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val serverState by viewModel.serverState.collectAsState()
    val activeModel by viewModel.activeLoadedModel.collectAsState()
    val allModels by viewModel.allModels.collectAsState()
    val cloudServers by viewModel.cloudServers.collectAsState()
    val operationMsg by viewModel.serverOperationMessage.collectAsState()

    var selectedSubTab by remember { mutableStateOf("SERVER") } // "SERVER", "TESTER", "CLOUD_BOX"
    var testPrompt by remember { mutableStateOf("Hello! Tell me about the current local model running on this device.") }
    var testStream by remember { mutableStateOf(true) }
    var testResponse by remember { mutableStateOf<String?>(null) }
    var testLoading by remember { mutableStateOf(false) }
    var testLatencyMs by remember { mutableStateOf(0L) }
    var portInput by remember { mutableStateOf(serverState.port.toString()) }
    var showModelPicker by remember { mutableStateOf(false) }

    val isRunning = serverState.status == ServerStatus.RUNNING
    val backendInfo = viewModel.getBackendInfoForModel(activeModel)
    val isCompatible = activeModel != null && backendInfo.isServerCompatible

    LaunchedEffect(operationMsg) {
        operationMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissServerOperationMessage()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        item {
            SoraSectionHeader(
                title = "Universal Model Server & Cloud",
                subtitle = "Local OpenAI-compatible REST API for GGUF, LiteRT, ONNX & Cloud Compute",
                icon = Icons.Default.Dns
            )
        }

        // Sub-navigation tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTabButton(
                    label = "Universal API Server",
                    icon = Icons.Default.Hub,
                    isSelected = selectedSubTab == "SERVER",
                    modifier = Modifier.weight(1f)
                ) { selectedSubTab = "SERVER" }

                FilterTabButton(
                    label = "API Test Console",
                    icon = Icons.Default.Terminal,
                    isSelected = selectedSubTab == "TESTER",
                    modifier = Modifier.weight(1f)
                ) { selectedSubTab = "TESTER" }

                FilterTabButton(
                    label = "Cloud Compute",
                    icon = Icons.Default.Cloud,
                    isSelected = selectedSubTab == "CLOUD_BOX",
                    modifier = Modifier.weight(1f)
                ) { selectedSubTab = "CLOUD_BOX" }
            }
        }

        // -------------------------------------------------------------
        // SUBTAB 1: UNIVERSAL API SERVER
        // -------------------------------------------------------------
        if (selectedSubTab == "SERVER") {
            // Main Server Status Card
            item {
                SoraGlassCard(
                    borderColor = when {
                        isRunning -> AccentGreen
                        isCompatible -> NeonCyan
                        else -> AccentRed.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.testTag("server_status_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isRunning) AccentGreen else if (isCompatible) AccentYellow else AccentRed
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRunning) "API Server Running" else "API Server Stopped",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRunning) AccentGreen else TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isRunning) "Listening on port ${serverState.port} • Serving local endpoints"
                                else if (isCompatible) "Ready to start • Model backend verified"
                                else "No compatible model loaded in memory",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Start/Stop Toggle
                        Switch(
                            checked = isRunning,
                            onCheckedChange = { viewModel.toggleApiServer() },
                            enabled = isCompatible || isRunning,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepDarkBg,
                                checkedTrackColor = AccentGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = GlassSurfaceVariant
                            ),
                            modifier = Modifier.testTag("server_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = GlassSurfaceVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Model & Backend Diagnostics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ACTIVE LOADED MODEL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeModel?.name ?: "No model loaded",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = { showModelPicker = !showModelPicker },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("switch_model_server_btn")
                        ) {
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (activeModel == null) "Load Model" else "Switch Model", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Format, Backend, Status Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoStatBadge(
                            label = "Format",
                            value = activeModel?.format ?: "None",
                            color = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )
                        InfoStatBadge(
                            label = "Backend",
                            value = backendInfo.backend,
                            color = NeonPurple,
                            modifier = Modifier.weight(1.2f)
                        )
                        InfoStatBadge(
                            label = "Status",
                            value = if (isCompatible) "Ready to serve" else "Load Model",
                            color = if (isCompatible) AccentGreen else AccentRed,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    // Model Picker Dialog / Expanded Section
                    AnimatedVisibility(visible = showModelPicker) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .background(GlassSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Select Model to Load into Memory:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val availableModels = allModels.filter { it.isDownloaded }
                            if (availableModels.isEmpty()) {
                                Text(
                                    text = "No models downloaded yet. Download models in the Downloads tab.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            } else {
                                availableModels.forEach { model ->
                                    val isSelected = activeModel?.id == model.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else GlassSurface)
                                            .clickable {
                                                viewModel.loadModelForServer(model)
                                                showModelPicker = false
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                SoraBadge(text = model.format, color = NeonCyan)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = model.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                            Text(
                                                text = "RAM: ${model.ramRequiredMb}MB • ${model.modelType}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        if (isSelected) {
                                            SoraBadge(text = "ACTIVE", color = AccentGreen)
                                        } else {
                                            Button(
                                                onClick = {
                                                    viewModel.loadModelForServer(model)
                                                    showModelPicker = false
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("load_model_pick_${model.id}")
                                            ) {
                                                Text("Load", fontSize = 11.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Connection Endpoints & Live URLs Card
            item {
                SoraGlassCard {
                    Text(
                        text = "API Base URLs (OpenAI Compatible)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connect Cursor, Open WebUI, Python OpenAI SDK, or local web apps to these endpoints:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Localhost URL
                    UrlCopyCard(
                        title = "Localhost Endpoint",
                        url = serverState.localUrl,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(serverState.localUrl))
                            Toast.makeText(context, "Copied Localhost URL!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Network IP URL
                    UrlCopyCard(
                        title = "Wi-Fi Network Endpoint (LAN)",
                        url = serverState.networkUrl,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(serverState.networkUrl))
                            Toast.makeText(context, "Copied Wi-Fi Network URL!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Cloudflare Public Tunnel URL if enabled
                    if (serverState.config.tunnelEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        UrlCopyCard(
                            title = "Public HTTPS Tunnel (Cloudflare)",
                            url = serverState.tunnelUrl ?: "https://${serverState.config.tunnelSubdomain}.trycloudflare.com/v1",
                            onCopy = {
                                val url = serverState.tunnelUrl ?: "https://${serverState.config.tunnelSubdomain}.trycloudflare.com/v1"
                                clipboardManager.setText(AnnotatedString(url))
                                Toast.makeText(context, "Copied Public Tunnel URL!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Live Performance & Telemetry Stats
            item {
                SoraGlassCard {
                    Text(
                        text = "Server Telemetry & Activity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricBox(
                            label = "Total Requests",
                            value = "${serverState.requestCount}",
                            color = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            label = "Tokens Generated",
                            value = "${serverState.tokensGenerated}",
                            color = AccentGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            label = "Uptime",
                            value = formatUptime(serverState.uptimeSeconds),
                            color = NeonPurple,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            label = "Last Latency",
                            value = "${serverState.lastLatencyMs}ms",
                            color = ElectricPink,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (serverState.lastRequestPath != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Last Route: ${serverState.lastRequestPath}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Security, Port & Tunnel Configuration
            item {
                SoraGlassCard {
                    Text(
                        text = "Security & Network Settings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Port Setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Server Port", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Default: 8080 (Range: 1024 - 65535)", fontSize = 11.sp, color = TextSecondary)
                        }

                        OutlinedTextField(
                            value = portInput,
                            onValueChange = {
                                portInput = it
                                it.toIntOrNull()?.let { p -> viewModel.updateServerPort(p) }
                            },
                            modifier = Modifier.width(100.dp).testTag("server_port_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = GlassSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Require API Key Switch & OpenAI Key Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "OpenAI-Compatible API Key Auth", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                SoraBadge(text = "OPENAI COMPATIBLE", color = AccentGreen)
                            }
                            Text(text = "Auto-generated for loaded model. Authenticates standard 'Bearer sk-proj-...' headers", fontSize = 11.sp, color = TextSecondary)
                        }

                        Switch(
                            checked = serverState.config.apiKeyEnabled,
                            onCheckedChange = { viewModel.updateApiKeyEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("api_key_switch")
                        )
                    }

                    if (serverState.config.apiKeyEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassSurfaceVariant, RoundedCornerShape(8.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Active Model API Key:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = serverState.config.apiKey,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = NeonCyan
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { viewModel.regenerateApiKey() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(serverState.config.apiKey))
                                        Toast.makeText(context, "OpenAI API Key copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = GlassSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Cloudflare Public Tunnel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Cloudflare Public Tunnel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Expose your local model server globally via HTTPS", fontSize = 11.sp, color = TextSecondary)
                        }

                        Switch(
                            checked = serverState.config.tunnelEnabled,
                            onCheckedChange = { viewModel.updateTunnelEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonPurple, checkedTrackColor = NeonPurple.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("tunnel_switch")
                        )
                    }
                }
            }

            // Supported OpenAI Routes Listing
            item {
                SoraGlassCard {
                    Text(
                        text = "Exposed REST Endpoints",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    EndpointRow(method = "GET", path = "/v1/models", desc = "List loaded model & backend specs", color = AccentGreen)
                    EndpointRow(method = "POST", path = "/v1/chat/completions", desc = "OpenAI chat format with token streaming (SSE)", color = NeonCyan)
                    EndpointRow(method = "POST", path = "/v1/completions", desc = "Prompt text completions", color = NeonPurple)
                    EndpointRow(method = "POST", path = "/v1/embeddings", desc = "384-dim vector embeddings generation", color = ElectricPink)
                    EndpointRow(method = "GET", path = "/health", desc = "Server heartbeat & health probe", color = TextSecondary)
                }
            }
        }

        // -------------------------------------------------------------
        // SUBTAB 2: API TEST CONSOLE
        // -------------------------------------------------------------
        if (selectedSubTab == "TESTER") {
            item {
                SoraGlassCard(borderColor = NeonCyan) {
                    Text(
                        text = "Live Model API Tester",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Test POST /v1/chat/completions directly against the local model server in real time.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = testPrompt,
                        onValueChange = { testPrompt = it },
                        label = { Text("Prompt / Message Content") },
                        modifier = Modifier.fillMaxWidth().testTag("test_prompt_input"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = testStream,
                                onCheckedChange = { testStream = it },
                                colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                            )
                            Text(text = "Stream Tokens (SSE)", fontSize = 12.sp, color = TextPrimary)
                        }

                        Button(
                            onClick = {
                                if (activeModel == null) {
                                    Toast.makeText(context, "Please load a model first!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                testLoading = true
                                testResponse = ""
                                scope.launch {
                                    val startT = System.currentTimeMillis()
                                    try {
                                        val engine = viewModel.activeEngine.value
                                        if (engine != null) {
                                            if (testStream && engine.supportsStreaming()) {
                                                engine.streamText(testPrompt).collect { chunk ->
                                                    testResponse = (testResponse ?: "") + chunk
                                                }
                                            } else {
                                                val res = engine.generateText(testPrompt)
                                                testResponse = res
                                            }
                                        } else {
                                            testResponse = "Error: No engine currently loaded"
                                        }
                                    } catch (e: Exception) {
                                        testResponse = "Error executing request: ${e.localizedMessage}"
                                    } finally {
                                        testLatencyMs = System.currentTimeMillis() - startT
                                        testLoading = false
                                    }
                                }
                            },
                            enabled = !testLoading,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.testTag("send_test_request_btn")
                        ) {
                            if (testLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DeepDarkBg, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("Send Request", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (testResponse != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = GlassSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Response Output", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                            Text(text = "Latency: ${testLatencyMs}ms", fontSize = 11.sp, color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepDarkBg, RoundedCornerShape(8.dp))
                                .border(1.dp, GlassSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = testResponse?.ifBlank { "[Empty output]" } ?: "",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Python OpenAI SDK Code Snippet Generator
            item {
                val pythonSnippet = """
from openai import OpenAI

# Initialize client pointing to local offline model server on Android
client = OpenAI(
    base_url="${serverState.networkUrl}",
    api_key="${serverState.config.apiKey}"
)

response = client.chat.completions.create(
    model="${activeModel?.name ?: "local-model"}",
    messages=[
        {"role": "system", "content": "You are an AI assistant running offline on Android."},
        {"role": "user", "content": "Explain quantum computing in one sentence."}
    ],
    temperature=0.7
)

print(response.choices[0].message.content)
                """.trimIndent()

                SoraGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Python (Official openai Library)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            SoraBadge(text = "OPENAI SDK", color = NeonCyan)
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(pythonSnippet))
                                Toast.makeText(context, "Copied Python OpenAI snippet!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepDarkBg, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = pythonSnippet,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = AccentGreen
                        )
                    }
                }
            }

            // cURL Code Snippet Generator
            item {
                val curlCommand = "curl ${serverState.networkUrl}/chat/completions \\\n" +
                        "  -H \"Content-Type: application/json\" \\\n" +
                        (if (serverState.config.apiKeyEnabled) "  -H \"Authorization: Bearer ${serverState.config.apiKey}\" \\\n" else "") +
                        "  -d '{\n" +
                        "    \"model\": \"${activeModel?.name ?: "local-model"}\",\n" +
                        "    \"messages\": [{\"role\": \"user\", \"content\": \"Hello world\"}],\n" +
                        "    \"temperature\": 0.7\n" +
                        "  }'"

                SoraGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "cURL Terminal Command", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            SoraBadge(text = "BASH / CURL", color = TextSecondary)
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(curlCommand))
                                Toast.makeText(context, "Copied cURL command!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepDarkBg, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = curlCommand,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SUBTAB 3: SORA CLOUD BOX NETWORK (DISTRIBUTED COMPUTE)
        // -------------------------------------------------------------
        if (selectedSubTab == "CLOUD_BOX") {
            item {
                SoraGlassCard(borderColor = NeonPurple) {
                    Text(
                        text = "Sora Cloud Box Network",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Offload heavy video renders to nearby local AI hardware over zero-latency Wi-Fi.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SoraGradientButton(
                        text = "SCAN LOCAL NETWORK (mDNS)",
                        icon = Icons.Default.WifiTethering,
                        modifier = Modifier.fillMaxWidth().testTag("scan_cloud_btn"),
                        onClick = { viewModel.scanSoraCloudServers() }
                    )
                }
            }

            item {
                Text(text = "Detected Compute Servers (${cloudServers.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            if (cloudServers.isEmpty()) {
                item {
                    SoraGlassCard {
                        Text(
                            text = "No local Sora Cloud servers detected on this Wi-Fi network yet. Tap 'SCAN LOCAL NETWORK'.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(cloudServers) { server ->
                    SoraGlassCard(
                        borderColor = if (server.isConnected) AccentGreen else GlassSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = server.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "IP: ${server.ipAddress}:${server.port} • Latency: ${server.latencyMs}ms", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "GPU: ${server.gpuModel}", fontSize = 11.sp, color = NeonPurple)
                            }

                            StatusIndicator(isConnected = server.isConnected)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "RAM: ${server.availableRamGb}GB / ${server.totalRamGb}GB Free", fontSize = 11.sp, color = TextSecondary)
                            Text(text = "Active Workers: ${server.activeUsers}", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonCyan else GlassSurface)
            .border(1.dp, if (isSelected) NeonCyan else GlassSurfaceVariant, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) DeepDarkBg else TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) DeepDarkBg else TextPrimary
            )
        }
    }
}

@Composable
fun InfoStatBadge(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GlassSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text = label.uppercase(), fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun MetricBox(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GlassSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
    }
}

@Composable
fun UrlCopyCard(
    title: String,
    url: String,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassSurfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = url,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = NeonCyan
        )
    }
}

@Composable
fun EndpointRow(
    method: String,
    path: String,
    desc: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(text = method, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = path, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = TextPrimary)
            Text(text = desc, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

fun formatUptime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) String.format("%02d:%02d:%02d", hrs, mins, secs)
    else String.format("%02d:%02d", mins, secs)
}
