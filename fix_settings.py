import sys

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

if "import androidx.compose.ui.platform.LocalUriHandler" not in content:
    content = content.replace("import androidx.compose.ui.platform.LocalClipboardManager", "import androidx.compose.ui.platform.LocalClipboardManager\nimport androidx.compose.ui.platform.LocalUriHandler")

target = """        item {
            SoraSectionHeader(
                title = "Settings","""

new_section = """        // ==========================================
        // 0. CREATOR SUPPORT
        // ==========================================
        item {
            val uriHandler = LocalUriHandler.current
            SoraGlassCard(borderColor = ElectricPink) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "SUPPORT THE CREATOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricPink,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            uriHandler.openUri("https://youtube.com/shorts/iseGrWemeZw?is=hRw6b8l2tjrZpvYh")
                        },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.SmartDisplay, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "@OneFactEndlessWonder", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "Watch the latest short!", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(16.dp))
                    }
                    Button(
                        onClick = { uriHandler.openUri("https://www.youtube.com/@OneFactEndlessWonder") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPink)
                    ) {
                        Text("Subscribe on YouTube")
                    }
                }
            }
        }

        item {
            SoraSectionHeader(
                title = "Settings","""

content = content.replace(target, new_section)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
