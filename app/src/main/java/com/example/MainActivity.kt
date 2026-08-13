package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: SoraMainViewModel by viewModels()

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            viewModel.trimMemory()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        viewModel.trimMemory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SoraAiStudioTheme {
                val currentTab by viewModel.selectedTab.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DeepDarkBg,
                    bottomBar = {
                        SoraNavigationBar(
                            selectedTab = currentTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(DeepDarkBg)
                    ) {
                        when (currentTab) {
                            SoraTab.HOME -> HomeScreen(viewModel = viewModel)
                            SoraTab.GENERATE -> GenerateScreen(viewModel = viewModel)
                            SoraTab.MODELS -> ModelsScreen(viewModel = viewModel)
                            SoraTab.DOWNLOADS -> DownloadsScreen(viewModel = viewModel)
                            SoraTab.GALLERY -> GalleryScreen(viewModel = viewModel)
                            SoraTab.PROJECTS -> ProjectsScreen(viewModel = viewModel)
                            SoraTab.EDITOR -> EditorScreen(viewModel = viewModel)
                            SoraTab.ASSISTANT -> AssistantScreen(viewModel = viewModel)
                            SoraTab.SORA_CLOUD -> SoraCloudScreen(viewModel = viewModel)
                            SoraTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoraNavigationBar(
    selectedTab: SoraTab,
    onTabSelected: (SoraTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = GlassSurface,
        contentColor = NeonCyan,
        edgePadding = 8.dp,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty() && selectedTab.ordinal < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    height = 3.dp,
                    color = NeonCyan
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        SoraTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .testTag("tab_${tab.route}"),
                text = {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getTabIcon(tab),
                            contentDescription = tab.title,
                            tint = if (isSelected) NeonCyan else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NeonCyan else TextSecondary
                        )
                    }
                }
            )
        }
    }
}

fun getTabIcon(tab: SoraTab): ImageVector {
    return when (tab) {
        SoraTab.HOME -> Icons.Default.Home
        SoraTab.GENERATE -> Icons.Default.VideoCall
        SoraTab.MODELS -> Icons.Default.FolderZip
        SoraTab.DOWNLOADS -> Icons.Default.CloudDownload
        SoraTab.GALLERY -> Icons.Default.PermMedia
        SoraTab.PROJECTS -> Icons.Default.Movie
        SoraTab.EDITOR -> Icons.Default.ContentCut
        SoraTab.ASSISTANT -> Icons.Default.Psychology
        SoraTab.SORA_CLOUD -> Icons.Default.Cloud
        SoraTab.SETTINGS -> Icons.Default.Settings
    }
}
