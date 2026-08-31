package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StoryProjectEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.components.SoraGlassCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedStoriesScreen(
    viewModel: SoraMainViewModel,
    onStorySelected: (StoryProjectEntity) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val allStories by viewModel.repository.storyProjectDao.getAllStoryProjects().collectAsStateWithLifecycle(initialValue = emptyList())
    val coroutineScope = rememberCoroutineScope()
    
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedStoryForDelete by remember { mutableStateOf<StoryProjectEntity?>(null) }
    var showStoryDetailsDialog by remember { mutableStateOf(false) }
    var selectedStoryDetails by remember { mutableStateOf<StoryProjectEntity?>(null) }
    var sortBy by remember { mutableStateOf("NEWEST") } // NEWEST, OLDEST, TITLE_A_Z, TITLE_Z_A
    
    val sortedStories = remember(allStories, sortBy) {
        when (sortBy) {
            "NEWEST" -> allStories.sortedByDescending { it.updatedAt }
            "OLDEST" -> allStories.sortedBy { it.updatedAt }
            "TITLE_A_Z" -> allStories.sortedBy { it.title }
            "TITLE_Z_A" -> allStories.sortedByDescending { it.title }
            else -> allStories
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Saved Stories",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${allStories.size} stories stored",
                                fontSize = 12.sp,
                                color = NeonCyan
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Sort/Filter menu */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepDarkBg,
                    titleContentColor = TextPrimary
                ),
                modifier = Modifier.border(0.5.dp, CardBorder, RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp))
            )
        },
        containerColor = DeepDarkBg,
        modifier = Modifier.testTag("saved_stories_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepDarkBg)
        ) {
            // Sort Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort by:", fontSize = 12.sp, color = TextSecondary)
                listOf("NEWEST", "OLDEST", "TITLE_A_Z").forEach { option ->
                    FilterChip(
                        selected = sortBy == option,
                        onClick = { sortBy = option },
                        label = { Text(when(option) {
                            "NEWEST" -> "Newest"
                            "OLDEST" -> "Oldest"
                            "TITLE_A_Z" -> "A → Z"
                            else -> option
                        }, fontSize = 10.sp) },
                        modifier = Modifier.height(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan
                        )
                    )
                }
            }

            Divider(color = CardBorder, thickness = 0.5.dp)

            // Stories List
            if (sortedStories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = NeonCyan.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No Stories Yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = "Create a new story from the Story Writer studio.\nYour stories will appear here.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(sortedStories) { story ->
                        SavedStoryCard(
                            story = story,
                            onSelect = {
                                selectedStoryDetails = story
                                showStoryDetailsDialog = true
                                onStorySelected(story)
                            },
                            onEdit = {
                                onStorySelected(story)
                            },
                            onDelete = {
                                selectedStoryForDelete = story
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation && selectedStoryForDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Story?", color = TextPrimary) },
            text = {
                Text(
                    "Delete \"${selectedStoryForDelete?.title}\"?\nThis action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            selectedStoryForDelete?.let {
                                viewModel.repository.storyProjectDao.deleteStoryProject(it)
                            }
                            showDeleteConfirmation = false
                            selectedStoryForDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteConfirmation = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurface)
                ) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = DeepDarkBg,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        )
    }

    // Story Details Dialog
    if (showStoryDetailsDialog && selectedStoryDetails != null) {
        StoryDetailsDialog(
            story = selectedStoryDetails!!,
            onDismiss = { showStoryDetailsDialog = false }
        )
    }
}

@Composable
fun SavedStoryCard(
    story: StoryProjectEntity,
    onSelect: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val createdDate = dateFormatter.format(Date(story.createdAt))
    val updatedDate = dateFormatter.format(Date(story.updatedAt))
    
    var showMenu by remember { mutableStateOf(false) }

    SoraGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("story_card_${story.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Title and Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = story.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Badge(
                            text = story.genre,
                            icon = Icons.Default.Category,
                            backgroundColor = NeonCyan.copy(alpha = 0.15f),
                            textColor = NeonCyan
                        )
                        Badge(
                            text = "${story.chapterCount} chapters",
                            icon = Icons.Default.MenuBook,
                            backgroundColor = NeonPurple.copy(alpha = 0.15f),
                            textColor = NeonPurple
                        )
                    }
                }

                // Menu Button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(GlassSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Story") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                showMenu = false
                                onSelect()
                            },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFFF4444)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4444)) }
                        )
                    }
                }
            }

            Divider(color = CardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)

            // Story Info Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoTag("Theme: ${story.theme}", modifier = Modifier.weight(1f))
                InfoTag("Audience: ${story.targetAudience}", modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoTag("Tone: ${story.tone}", modifier = Modifier.weight(1f))
                InfoTag("POV: ${story.pointOfView}", modifier = Modifier.weight(1f))
            }

            // Footer with Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Created: $createdDate",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Updated: $updatedDate",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun StoryDetailsDialog(
    story: StoryProjectEntity,
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(story.title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow("Genre", story.genre)
                DetailRow("Theme", story.theme)
                DetailRow("Tone", story.tone)
                DetailRow("Chapters", "${story.chapterCount}")
                DetailRow("Target Audience", story.targetAudience)
                DetailRow("Point of View", story.pointOfView)
                DetailRow("Language", story.language)
                
                if (story.setting.isNotEmpty()) {
                    DetailRow("Setting", story.setting)
                }
                if (story.mainConflict.isNotEmpty()) {
                    DetailRow("Main Conflict", story.mainConflict)
                }
                if (story.worldMemory.isNotEmpty()) {
                    Divider(color = CardBorder)
                    Text("World Lore:", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                    Text(story.worldMemory, color = TextSecondary, fontSize = 10.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f))
            ) {
                Text("Close", color = NeonCyan)
            }
        },
        containerColor = DeepDarkBg,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    )
}

@Composable
fun Badge(
    text: String,
    icon: ImageVector? = null,
    backgroundColor: Color = NeonCyan.copy(alpha = 0.15f),
    textColor: Color = NeonCyan
) {
    Row(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
        }
        Text(text, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun InfoTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .background(GlassSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        fontSize = 10.sp,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NeonCyan)
        Text(value, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
