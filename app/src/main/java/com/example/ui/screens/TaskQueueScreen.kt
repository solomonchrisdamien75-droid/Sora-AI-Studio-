package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GenerationJobEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskQueueScreen(viewModel: SoraMainViewModel) {
    val allJobs by viewModel.allJobs.collectAsState()
    val queuedJobs by viewModel.queuedJobs.collectAsState()
    val isQueueProcessing by viewModel.isQueueProcessing.collectAsState()
    val isAutoProcess by viewModel.isAutoProcessEnabled.collectAsState()
    val runningJobId by viewModel.currentRunningJobId.collectAsState()
    val statusMsg by viewModel.queueStatusMessage.collectAsState()

    var showBatchDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "QUEUED", "RUNNING", "COMPLETED", "FAILED"

    val filteredJobs = remember(allJobs, selectedFilter) {
        when (selectedFilter) {
            "QUEUED" -> allJobs.filter { it.status == "QUEUED" }
            "RUNNING" -> allJobs.filter { it.status == "RUNNING" }
            "COMPLETED" -> allJobs.filter { it.status == "COMPLETED" }
            "FAILED" -> allJobs.filter { it.status == "FAILED" || it.status == "CANCELLED" }
            else -> allJobs
        }
    }

    val runningJob = remember(allJobs, runningJobId) {
        allJobs.firstOrNull { it.status == "RUNNING" || it.id == runningJobId }
    }

    val completedCount = remember(allJobs) { allJobs.count { it.status == "COMPLETED" } }
    val queuedCount = remember(allJobs) { allJobs.count { it.status == "QUEUED" } }
    val failedCount = remember(allJobs) { allJobs.count { it.status == "FAILED" || it.status == "CANCELLED" } }
    val totalEstimatedSec = remember(allJobs) {
        allJobs.filter { it.status == "QUEUED" || it.status == "RUNNING" }.sumOf { it.durationSeconds * 2 }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        item {
            SoraSectionHeader(
                title = "AI Video Task Queue",
                subtitle = "Batch generator & sequential offline background renderer",
                icon = Icons.Default.Queue
            )
        }

        // Status Toast Banner
        if (statusMsg != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = statusMsg ?: "", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { viewModel.dismissQueueStatusMessage() }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Queue Control & Stats Hero Card
        item {
            SoraGlassCard(borderColor = if (isQueueProcessing) AccentGreen else NeonCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isQueueProcessing) AccentGreen else AccentYellow)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isQueueProcessing) "SEQUENTIAL WORKER ACTIVE" else "QUEUE STANDBY / PAUSED",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isQueueProcessing) AccentGreen else AccentYellow
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (runningJob != null) "Processing: ${runningJob.title}" else "${queuedCount} task(s) awaiting execution",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isQueueProcessing) {
                            Button(
                                onClick = { viewModel.pauseQueueProcessing() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("pause_queue_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause", color = DeepDarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startQueueProcessing() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                shape = RoundedCornerShape(8.dp),
                                enabled = queuedCount > 0 || runningJob != null,
                                modifier = Modifier.testTag("start_queue_btn")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Process Queue", color = DeepDarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mini Stat Metrics Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(label = "Queued", value = "$queuedCount", color = NeonCyan, modifier = Modifier.weight(1f))
                    StatBox(label = "Running", value = if (runningJob != null) "1" else "0", color = AccentGreen, modifier = Modifier.weight(1f))
                    StatBox(label = "Completed", value = "$completedCount", color = NeonPurple, modifier = Modifier.weight(1f))
                    StatBox(label = "Est. Total", value = "${totalEstimatedSec}s", color = ElectricPink, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto Process Toggle & Offline persistence note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = "Auto-Process Sequentially", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Automatically execute next job in order", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = isAutoProcess,
                        onCheckedChange = { viewModel.toggleAutoProcessQueue(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("auto_process_switch")
                    )
                }
            }
        }

        // Active Real-time Job Progress Monitor Card
        if (runningJob != null) {
            item {
                SoraGlassCard(borderColor = AccentGreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                progress = { runningJob.progressPercent / 100f },
                                modifier = Modifier.size(22.dp),
                                color = AccentGreen,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rendering Active Job: ${runningJob.currentFrame} / ${runningJob.totalFrames} frames",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }
                        SoraBadge(text = "${runningJob.progressPercent}%", color = AccentGreen)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = runningJob.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = runningJob.prompt, fontSize = 11.sp, color = TextSecondary, maxLines = 2)

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { runningJob.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = AccentGreen,
                        trackColor = GlassSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SoraBadge(text = runningJob.resolution, color = NeonCyan)
                            SoraBadge(text = "${runningJob.durationSeconds}s", color = NeonPurple)
                            SoraBadge(text = runningJob.backendUsed, color = ElectricPink)
                        }
                        OutlinedButton(
                            onClick = { viewModel.cancelQueuedJob(runningJob.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel Job", fontSize = 11.sp, color = AccentRed)
                        }
                    }
                }
            }
        }

        // Action Toolbar: + Batch Queue & Clear Completed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showBatchDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("open_batch_creator_btn")
                ) {
                    Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Batch Queue Jobs", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.addCurrentFormToQueue() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("queue_current_form_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Queue Current Form", fontSize = 12.sp, color = NeonCyan)
                }

                if (completedCount > 0 || failedCount > 0) {
                    IconButton(
                        onClick = { viewModel.clearCompletedJobs() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlassSurface)
                            .border(1.dp, GlassSurfaceVariant, RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear Finished", tint = TextSecondary)
                    }
                }
            }
        }

        // Filter Tabs Strip
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item { FilterChipItem("All (${allJobs.size})", "ALL", selectedFilter) { selectedFilter = "ALL" } }
                item { FilterChipItem("Queued (${queuedCount})", "QUEUED", selectedFilter) { selectedFilter = "QUEUED" } }
                item { FilterChipItem("Running (${if (runningJob != null) 1 else 0})", "RUNNING", selectedFilter) { selectedFilter = "RUNNING" } }
                item { FilterChipItem("Completed (${completedCount})", "COMPLETED", selectedFilter) { selectedFilter = "COMPLETED" } }
                item { FilterChipItem("Failed/Cancelled (${failedCount})", "FAILED", selectedFilter) { selectedFilter = "FAILED" } }
            }
        }

        // Empty state
        if (filteredJobs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Checklist, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "No tasks found in this view", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Use '+ Batch Queue Jobs' or '+ Add to Queue' on the Generate screen", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }

        // List of Queued & History Jobs
        items(filteredJobs, key = { it.id }) { job ->
            val isJobRunning = job.status == "RUNNING" || job.id == runningJobId
            val isJobQueued = job.status == "QUEUED"

            SoraGlassCard(
                borderColor = when (job.status) {
                    "RUNNING" -> AccentGreen
                    "QUEUED" -> NeonCyan.copy(alpha = 0.6f)
                    "COMPLETED" -> AccentGreen.copy(alpha = 0.4f)
                    "FAILED" -> AccentRed
                    else -> GlassSurfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoraBadge(
                            text = job.status,
                            color = when (job.status) {
                                "RUNNING" -> AccentGreen
                                "QUEUED" -> NeonCyan
                                "COMPLETED" -> AccentGreen
                                "FAILED" -> AccentRed
                                else -> TextSecondary
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = job.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Order Shift & Action buttons for Queued jobs
                    if (isJobQueued) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.moveQueuedJob(job.id, moveUp = true) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { viewModel.moveQueuedJob(job.id, moveUp = false) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { viewModel.deleteQueuedJob(job.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else if (job.status == "FAILED" || job.status == "CANCELLED") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.retryQueuedJob(job.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.Replay, contentDescription = "Retry", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { viewModel.deleteQueuedJob(job.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else if (job.status == "COMPLETED") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                viewModel.addClipToEditor("renders/${job.id}.mp4", job.title)
                                viewModel.selectTab(SoraTab.EDITOR)
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.ContentCut, contentDescription = "Edit in Studio", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { viewModel.selectTab(SoraTab.GALLERY) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.PlayCircle, contentDescription = "View in Gallery", tint = AccentGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(text = job.prompt, fontSize = 12.sp, color = TextSecondary, maxLines = 2)

                if (isJobRunning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { job.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = AccentGreen,
                        trackColor = GlassSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SoraBadge(text = job.resolution, color = NeonCyan)
                        SoraBadge(text = "${job.durationSeconds}s", color = NeonPurple)
                        SoraBadge(text = job.mode, color = ElectricPink)
                    }

                    val dateStr = remember(job.createdAt) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(job.createdAt))
                    }
                    Text(text = "Queued at $dateStr", fontSize = 10.sp, color = TextSecondary)
                }
            }
        }
    }

    // Batch Job Creator Modal Dialog
    if (showBatchDialog) {
        BatchJobCreatorDialog(
            onDismiss = { showBatchDialog = false },
            onBatchQueue = { prefix, prompts, mode, duration, res, fps ->
                viewModel.addBatchJobsToQueue(prefix, prompts, "TEXT_TO_VIDEO", mode, duration, res, fps)
                showBatchDialog = false
            }
        )
    }
}

@Composable
fun BatchJobCreatorDialog(
    onDismiss: () -> Unit,
    onBatchQueue: (String, List<String>, String, Int, String, Int) -> Unit
) {
    var prefix by remember { mutableStateOf("Sora Storyboard") }
    var multiPromptText by remember {
        mutableStateOf(
            "Scene 1: Drone camera descending over neon cyberpunk skyscrapers at sunset\n" +
            "Scene 2: Protagonist walking through rain-slicked alleyways under glowing holographic billboards\n" +
            "Scene 3: Flying speeder vehicle accelerating into hyperdrive light speed corridor"
        )
    }
    var selectedMode by remember { mutableStateOf("FAST") }
    var selectedDuration by remember { mutableStateOf(5) }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableStateOf(24) }

    val parsedPrompts = remember(multiPromptText) {
        multiPromptText.lines().map { it.trim() }.filter { it.isNotBlank() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null, tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Batch Queue Generator", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Queue multiple AI video generation prompts to render sequentially in the background while offline:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Batch Project Title Prefix") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Prompts (1 prompt per line = 1 video task):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                OutlinedTextField(
                    value = multiPromptText,
                    onValueChange = { multiPromptText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                )

                // Quick Prompt Preset Chips
                Text(text = "Quick Storyboard Templates:", fontSize = 11.sp, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        OutlinedButton(
                            onClick = {
                                multiPromptText = "Shot 1: Deep space nebula vortex expanding\nShot 2: Exploration starship exiting warp gate\nShot 3: Alien planet surface with twin suns"
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Sci-Fi Trilogy", fontSize = 10.sp)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                multiPromptText = "Panel 1: Anime warrior drawing mystical katana with thunder sparks\nPanel 2: Dynamic 3D dash slash through enemy armor\nPanel 3: Sheathing katana as sonic shockwave ripples"
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Anime Combat", fontSize = 10.sp)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                multiPromptText = "Variation 1: Drone wide establishing shot over alpine lake at dawn\nVariation 2: Fast low-altitude fpv flyby above crystal water\nVariation 3: Sunset panoramic golden hour horizon"
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Drone Vistas", fontSize = 10.sp)
                        }
                    }
                }

                // Batch Duration & Resolution Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Duration / Clip", fontSize = 11.sp, color = TextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(listOf(1, 5, 10, 30)) { sec ->
                                val isSelected = selectedDuration == sec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) NeonCyan else GlassSurface)
                                        .clickable { selectedDuration = sec }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "${sec}s", fontSize = 11.sp, color = if (isSelected) DeepDarkBg else TextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Resolution", fontSize = 11.sp, color = TextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(listOf("720p", "1080p", "4K")) { res ->
                                val isSelected = selectedResolution == res
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) NeonPurple else GlassSurface)
                                        .clickable { selectedResolution = res }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = res, fontSize = 11.sp, color = if (isSelected) DeepDarkBg else TextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onBatchQueue(prefix, parsedPrompts, selectedMode, selectedDuration, selectedResolution, selectedFps)
                },
                enabled = parsedPrompts.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text("Queue ${parsedPrompts.size} Tasks", color = DeepDarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun StatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = label, fontSize = 9.sp, color = TextSecondary)
        }
    }
}

@Composable
fun FilterChipItem(
    title: String,
    filterKey: String,
    selectedFilter: String,
    onClick: () -> Unit
) {
    val isSelected = filterKey == selectedFilter
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else GlassSurface)
            .border(1.dp, if (isSelected) NeonCyan else GlassSurfaceVariant, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) NeonCyan else TextSecondary
        )
    }
}
