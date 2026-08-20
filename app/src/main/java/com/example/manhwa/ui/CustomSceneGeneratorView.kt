package com.example.manhwa.ui

import androidx.compose.runtime.Composable
import com.example.manhwa.model.ManhwaProject
import com.example.ui.SoraMainViewModel
import com.example.ui.components.CustomSceneChatGeneratorView

@Composable
fun CustomSceneGeneratorView(
    viewModel: SoraMainViewModel,
    project: ManhwaProject,
    onProjectUpdated: (ManhwaProject) -> Unit
) {
    CustomSceneChatGeneratorView(
        viewModel = viewModel,
        studioTitle = "Manhwa Custom Scene Studio"
    )
}
