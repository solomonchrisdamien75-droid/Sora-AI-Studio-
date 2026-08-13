package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SoraMainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Composable
fun ProjectsScreen(viewModel: SoraMainViewModel) {
    val projects by viewModel.projects.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Film & Story Projects",
                subtitle = "Manage long-form AI movie production pipelines",
                icon = Icons.Default.Movie
            )
        }

        item {
            SoraGradientButton(
                text = "+ CREATE NEW FILM PROJECT",
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.selectTab(com.example.ui.SoraTab.ASSISTANT) }
            )
        }

        if (projects.isEmpty()) {
            item {
                SoraGlassCard {
                    Text(
                        text = "No active movie projects yet. Use the AI Assistant to generate a script and storyboard pipeline.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(projects) { project ->
                SoraGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = project.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "${project.sceneCount} Scenes • ${project.durationSeconds}s Total Duration", fontSize = 12.sp, color = TextSecondary)
                        }
                        SoraBadge(text = "In Progress", color = NeonCyan)
                    }
                }
            }
        }
    }
}
