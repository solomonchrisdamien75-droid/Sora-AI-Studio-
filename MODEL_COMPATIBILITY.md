# Model Compatibility Specification

This document details supported model formats, hardware requirements, and execution backends for Sora AI Studio.

---

## Supported Formats & Engines

| Format | Backend Engine | Primary Acceleration | Supported Workloads |
| :--- | :--- | :--- | :--- |
| **GGUF** | llama.cpp / GGUF Bridge | CPU + NNAPI | Offline Scriptwriting, Prompt Improvement, Storyboard Planning |
| **LiteRT / TFLite** | LiteRT Engine | Vulkan 1.3 GPU | Fast Mode Video Generation, Image AI (SD 1.5) |
| **ONNX** | ONNX Runtime | DirectML / Vulkan | Balanced & Cinema Mode Video Generation |
| **SafeTensors** | Diffusers Converter | GPU Shader Pipeline | High Detail Image & Video Frames |

---

## Device Performance Tiers

### 1. Low-End Tier (3 - 4 GB RAM)
- **Max Recommended Model RAM**: ~2.2 GB
- **Supported Models**: LiteRT Fast Video (1.1GB), SD 1.5 LiteRT (980MB), Gemma 2B GGUF Q4 (1.2GB)
- **Recommended Resolution**: 720p @ 24fps
- **Delegation**: Offloads heavy rendering to local Sora Cloud server when available.

### 2. Mid-Range Tier (6 - 8 GB RAM)
- **Max Recommended Model RAM**: ~4.2 GB
- **Supported Models**: Wan 2.1 Video 1.3B GGUF, LTX Video 0.9.1 ONNX
- **Recommended Resolution**: 1080p @ 24fps / 30fps

### 3. High-End Tier (12 GB+ RAM)
- **Max Recommended Model RAM**: ~7.5 GB+
- **Supported Models**: CogVideoX, Hunyuan Video, Flux Image Pipelines
- **Recommended Resolution**: 1080p / 4K @ 30fps / 60fps
