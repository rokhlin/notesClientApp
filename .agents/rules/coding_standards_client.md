# Rule: Client-Side Coding Standards (Kotlin Multiplatform & Compose)

## 1. General Kotlin Conventions
- **Style Guide:** Strictly adhere to the official [JetBrains Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- **Immutability:** Use `val` by default. Only use `var` when mutability is strictly required. Prefer immutable collections (`List`, `Map`) over mutable variants.
- **Formatting:** Enforce a 4-space indentation. All code must pass `ktlint` and `detekt` static analysis checks without warnings.

## 2. Dependency Injection (Koin)
- **Framework:** The project exclusively utilizes **Koin** for Dependency Injection.
- **Implementation:** Modules must be logically separated (e.g., `networkModule`, `databaseModule`, `viewModelModule`). Avoid field injection where constructor injection is possible.

## 3. Concurrency and Coroutines
- **Structured Concurrency:** Never use `GlobalScope`. All coroutines must be launched within an appropriate `CoroutineScope` tied to the lifecycle (e.g., `viewModelScope` in ViewModels).
- **Dispatchers:** 
  - `Dispatchers.Main` for UI updates and state mutations.
  - `Dispatchers.IO` for database operations, file I/O, and network requests.
  - `Dispatchers.Default` for heavy CPU-bound computational tasks.

## 4. MVVM Architecture and State Management
- **Pattern:** The client strictly follows the **Model-View-ViewModel (MVVM)** architecture.
- **UI State Handling:** ViewModels must expose a single source of truth for the UI using `StateFlow`.
- **State Encapsulation:** The state must be represented using a sealed interface to guarantee robust error and loading handling:
```kotlin
  sealed interface UiState<out T> {
      data object Loading : UiState<Nothing>
      data class Success<T>(val data: T) : UiState<T>
      data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
```
 

## 5. Logging and Monitoring (LoggerManager)
- **Centralized Logger:** The AI must implement a `LoggerManager` abstraction (e.g., wrapping KMP-compatible libraries like Kermit or Napier) to centrally configure log levels (DEBUG, INFO, WARN, ERROR) based on the build variant (Debug vs. Release).
- **Annotation-Based Auto-Logging:** Implement a mechanism (via KSP or Kotlin compiler plugins) supporting a custom `@Loggable` annotation. When applied to public methods or classes, it must automatically intercept and log the method invocation, input arguments, execution time, and the return value/response.
- **Inline Logging:** The `LoggerManager` must expose a clean, idiomatic API for manual, contextual logging within function bodies when specific execution details are needed.
- **Error Visualization & Reporting:** Any log recorded at the `ERROR` level must trigger a dual-action pipeline:
  1. **Remote Tracking:** Automatically route the error and stack trace to a remote crash reporting tool (e.g., Firebase Crashlytics, Sentry).
  2. **Visual Feedback:** Trigger a global UI event (e.g., via a shared `ErrorEventBus` or global `UiState`) so the application can visually reflect the error (e.g., showing a Snackbar to the user, or a detailed Debug Overlay in development mode).