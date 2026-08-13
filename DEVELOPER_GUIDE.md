# Developer Guide - Sora AI Studio

## Codebase Architecture
Sora AI Studio is constructed with Jetpack Compose, Kotlin 2.2, Room, and Coroutines Flow.

### Building and Testing
- Run `compile_applet` tool to perform build verification.
- Run unit tests with Gradle:
  ```bash
  gradle :app:testDebugUnitTest
  ```

### Key ViewModel Flow
State in `SoraMainViewModel` is exposed via `StateFlow` and consumed in Jetpack Compose via `collectAsStateWithLifecycle()`.

```kotlin
val allModels: StateFlow<List<AiModelEntity>> = repository.aiModelDao.getAllModels()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

### Adding a New AI Inference Engine
1. Implement the `AIInferenceEngine` interface in `com.example.ai.inference`.
2. Register the engine in `InferenceEngineManager.kt`.
3. Map supported file formats (e.g. `.ckpt`, `.tflite`, `.gguf`) in `selectEngineForModel()`.
