# User Guide - Sora AI Studio

Welcome to **Sora AI Studio**, your offline-first AI video generation workstation for Android.

---

## Core Workbench & Tabs

### 1. Home
- Displays real-time hardware status: RAM usage, CPU core breakdown, Vulkan GPU support, and thermal status.
- Quick workbench launchers for Text-to-Video, Image-to-Video, and the Offline AI Assistant.
- Displays currently installed local models and recent renders.

### 2. Generate (AI Workbench)
- **Generation Types**: Text-to-Video, Image-to-Video, Video-to-Video, Image AI, and Upscaling.
- **Quality Modes**:
  - *Fast Mode*: Uses LiteRT/Vulkan for rapid frame rendering with minimal memory footprint.
  - *Balanced Mode*: Uses ONNX/CPU+NNAPI for balanced frame quality.
  - *Cinema Mode*: High-resolution Vulkan rendering for crisp details.
- **Real-Time Render Progress**: Monitor current frame, FPS, GPU memory, and backend status live while generating.

### 3. Models
- Complete Model Manager to view, filter (GGUF, LiteRT, ONNX), validate, and benchmark models.
- Automatic hardware compatibility checking prevents out-of-memory crashes by inspecting available device RAM before model loading.

### 4. Downloads (Hugging Face)
- Directly search, browse, and download open-source models (GGUF, LiteRT, ONNX) from Hugging Face.
- Live progress indicator showing download speed (MB/s), ETA, and completed bytes.

### 5. Gallery
- View all generated videos and images stored on device.
- Select any item to view detailed prompts or send the clip directly to the Video Editor.

### 6. Projects & Editor
- Timeline Video Editor with multi-track support.
- Split clips, apply cyberpunk/neon filters, adjust playback speed (0.5x to 2.0x), select aspect ratio (16:9, 9:16, 21:9), and export at 1080p or 4K.

### 7. AI Assistant
- Offline scriptwriter and shot planner powered by local LLM models.
- Generates a complete movie script, camera movement breakdown, lighting instructions, sound effects, and prompt text.
- One-tap button to transfer any generated shot prompt straight into the AI Workbench.

### 8. Sora Cloud
- Discover nearby Sora Cloud Box servers on your local Wi-Fi network using mDNS.
- Offload intensive video generation jobs to dedicated local hardware.
