# Sora AI Studio & Sora Cloud

**Sora AI Studio** is a native Android AI video generation workstation and ecosystem built for on-device AI video creation and local network private AI server compute.

---

## Key Features

1. **Every Major AI Generation Mode**:
   - Text-to-Video
   - Image-to-Video
   - Video-to-Video
   - Image AI & Inpainting
   - AI Upscaling & Video Enhancement

2. **On-Device & Offline First**:
   - Runs downloaded AI models (GGUF, LiteRT, ONNX, SafeTensors) completely on-device without internet connection after model installation.

3. **Intelligent Hardware Detection & Virtual Memory**:
   - Automatically detects CPU cores, total/available RAM, GPU Vulkan 1.3, NNAPI, thermal state, and storage.
   - Categorizes devices into 3GB Low RAM, 6GB Mid Tier, and 12GB+ Ultra Tier with custom RAM safety checks.
   - SD Card Workspace for model caching and frame rendering.

4. **Model Manager & Hugging Face Browser**:
   - Scan internal & SD card model directories.
   - Built-in Hugging Face browser with direct background downloads, speed meter, ETA, and checksum verification.

5. **Offline AI Assistant**:
   - Scriptwriter, storyboard builder, camera/lighting suggestions, and shot prompt synthesis using local LLM models (e.g. Gemma GGUF via llama.cpp bridge).

6. **Professional Timeline Video Editor**:
   - Multi-track timeline, trim/split clips, cyberpunk filters, playback speed adjusters (0.5x - 2.0x), aspect ratio presets (16:9, 9:16, 21:9), and 1080p/4K export presets.

7. **Sora Cloud Architecture**:
   - Local network mDNS discovery for pairing with a nearby Sora Cloud server or future Sora Cloud Box over Wi-Fi.

---

## Tech Stack
- **Language**: Kotlin 2.2
- **UI Framework**: Jetpack Compose (Material Design 3 Dark Theme)
- **Database**: Room (KSP code generation)
- **Networking**: OkHttp, Retrofit
- **AI Runtimes**: LiteRT / TFLite, llama.cpp GGUF, ONNX Runtime
- **Build System**: Gradle Kotlin DSL (.gradle.kts)
