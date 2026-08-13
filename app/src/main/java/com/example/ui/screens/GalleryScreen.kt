package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.data.GalleryItemEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.theme.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Composable
fun GalleryScreen(viewModel: SoraMainViewModel) {
    val items by viewModel.galleryItems.collectAsState()
    var selectedItem by remember { mutableStateOf<GalleryItemEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SoraSectionHeader(
            title = "Generated Gallery",
            subtitle = "${items.size} videos and images saved on device",
            icon = Icons.Default.PermMedia
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            SoraGlassCard {
                Text(
                    text = "No generated media found. Start a new AI rendering task in the Workbench.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassSurface)
                            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { selectedItem = item }
                            .padding(10.dp)
                            .testTag("gallery_item_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SoraBadge(text = item.resolutionLabel, color = ElectricPink)
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Play",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = item.prompt,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        // Details & Export Dialog
        if (selectedItem != null) {
            AlertDialog(
                onDismissRequest = { selectedItem = null },
                containerColor = GlassSurface,
                title = {
                    Text(text = selectedItem!!.title, color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(text = "Prompt:", fontSize = 12.sp, color = NeonCyan)
                        Text(text = selectedItem!!.prompt, fontSize = 13.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Resolution: ${selectedItem!!.resolutionLabel}", fontSize = 12.sp, color = TextSecondary)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addClipToEditor(selectedItem!!.filePath, selectedItem!!.title)
                            selectedItem = null
                            viewModel.selectTab(SoraTab.EDITOR)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Send to Video Editor", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedItem = null }) {
                        Text("Close", color = TextSecondary)
                    }
                }
            )
        }
    }
}
