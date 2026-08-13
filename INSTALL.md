# Installation Guide - Sora AI Studio

## System Requirements
- **OS**: Android 9.0 (API Level 28) or higher (Android 14+ recommended)
- **Architecture**: arm64-v8a or x86_64
- **Minimum RAM**: 3 GB (4 GB+ recommended for 1080p rendering)
- **Storage**: At least 2 GB free internal storage (External SD card supported for model caching)
- **GPU**: Vulkan 1.1+ support recommended for accelerated AI inference

---

## Building from Source

### Prerequisites
1. Android Studio Ladybug (2024.2.1) or newer
2. JDK 17 or higher
3. Android SDK 36 (minor API level 1)

### Steps
1. Clone or extract the project repository.
2. Open Android Studio and select **Open an Existing Project**, pointing to the root folder.
3. Allow Gradle to sync dependencies automatically.
4. Connect an Android device with USB Debugging enabled, or launch a Vulkan-enabled Android Emulator.
5. Select the `:app` run configuration and click **Run** (`Shift + F10`).

---

## Installing On-Device AI Models
1. Launch Sora AI Studio on your device.
2. Navigate to the **Downloads** tab to search and download open-source models from Hugging Face directly into your device's model storage.
3. Alternatively, copy `.gguf`, `.tflite`, or `.onnx` model files into `/sdcard/Android/data/com.aistudio.soraaistudio.wqvzx/files/ai_models/`.
4. Open the **Models** tab and tap **Folder Scan** to register imported models.
