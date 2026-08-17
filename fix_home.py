import sys
import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

# Add imports
if "import androidx.compose.ui.platform.LocalUriHandler" not in content:
    content = content.replace("import androidx.compose.ui.platform.testTag", "import androidx.compose.ui.platform.testTag\nimport androidx.compose.ui.platform.LocalUriHandler")

target = """        // Active Task Queue Status Banner (if tasks are queued)"""

new_banner = """        // Support the Creator Banner
        item {
            val uriHandler = LocalUriHandler.current
            SoraGlassCard(borderColor = ElectricPink) {
                Column(
                    modifier = Modifier.fillMaxWidth().clickable {
                        uriHandler.openUri("https://youtube.com/shorts/iseGrWemeZw?is=hRw6b8l2tjrZpvYh")
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SmartDisplay, contentDescription = "YouTube", tint = ElectricPink)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Support the Creator!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricPink
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Subscribe to @OneFactEndlessWonder and watch the latest short!",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            uriHandler.openUri("https://www.youtube.com/@OneFactEndlessWonder")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPink.copy(alpha = 0.8f))
                    ) {
                        Text("Visit Channel")
                    }
                }
            }
        }

        // Active Task Queue Status Banner (if tasks are queued)"""

content = content.replace(target, new_banner)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)
