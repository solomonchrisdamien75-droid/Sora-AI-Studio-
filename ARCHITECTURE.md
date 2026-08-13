# Architecture Guide - Sora AI Studio

Sora AI Studio follows Clean Architecture & MVVM with Kotlin Coroutines, Flow, Jetpack Compose, and Room local database persistence.

---

## Architectural Layers

```
                                  [ Jetpack Compose UI ]
                                            │
                                  [ SoraMainViewModel ]
                                            │
                                    [ SoraRepository ]
                                            │
 ┌─────────────────┬────────────────┬───────┴────────┬─────────────────┬────────────────┐
 │                 │                │                │                 │                │
[HardwareDetector] [InferenceManager][ModelDownloader] [OfflineAssistant][VideoEditorEngine][SoraCloudClient]
 │                 │                │                │                 │                │
[Android OS APIs]  [LiteRT/GGUF/ONNX][HuggingFace API] [Local LLM Engine][Timeline Engine][Local Network mDNS]
                                            │
                                   [ Room AppDatabase ]
```

---

## Module Overview

- `com.example.data`:
  - `Entities.kt`: Room database schema for `AiModelEntity`, `GenerationJobEntity`, `ProjectEntity`, `SoraCloudServerEntity`, and `GalleryItemEntity`.
  - `Daos.kt`: Asynchronous Flow-based DAOs for local state persistence.
  - `AppDatabase.kt`: Room database instance.
  - `SoraRepository.kt`: Single source of truth unifying local database, hardware profile, inference engines, and network clients.

- `com.example.ai`:
  - `hardware/HardwareDetector.kt`: Hardware profile scanner for RAM, CPU, GPU Vulkan, NNAPI, thermal status, and battery.
  - `inference/AIInferenceEngine.kt`: Abstract inference engine interface.
  - `inference/LiteRTEngine.kt`: Native LiteRT / Vulkan inference implementation.
  - `inference/LlamaCppEngine.kt`: GGUF text generation bridge.
  - `inference/OnnxEngine.kt`: ONNX Runtime DirectML/Vulkan bridge.
  - `inference/InferenceEngineManager.kt`: Automatic engine router based on model format and device hardware limits.
  - `downloader/`: Hugging Face client & download manager.
  - `assistant/`: Offline scriptwriter and shot planner engine.

- `com.example.editor`:
  - `VideoEditorEngine.kt`: Non-destructive timeline editor handling clip splitting, filtering, speed adjustment, and aspect ratio formatting.

- `com.example.cloud`:
  - `SoraCloudClient.kt`: Local network mDNS discovery client and job submission interface.

- `com.example.ui`:
  - `SoraMainViewModel.kt`: Central StateFlow manager.
  - `screens/`: Composables for Home, Generate, Models, Downloads, Gallery, Projects, Editor, Assistant, SoraCloud, and Settings.
  - `components/`: Glassmorphism cards, badges, section headers, gradient buttons, and status indicators.
