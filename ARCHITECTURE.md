# Daymark - System Architecture & Engineering Blueprint

## 1. Architectural Overview

Daymark follows modern Android architecture best practices: **Clean Architecture** principles separated into distinct logical layers, combined with the **Model-View-ViewModel (MVVM)** pattern and **Unidirectional Data Flow (UDF)**.

```
                    ┌───────────────────────────────┐
                    │       Presentation Layer      │
                    │   (Jetpack Compose + MVVM)    │
                    └───────────────┬───────────────┘
                                    │ Observes StateFlow<UiState>
                                    │ Emits UiEvents
                                    ▼
                    ┌───────────────────────────────┐
                    │          Domain Layer         │
                    │  (Models, UseCases, Contracts)│
                    └───────────────┬───────────────┘
                                    │ Requests Data / Executes Business Rules
                                    ▼
                    ┌───────────────────────────────┐
                    │           Data Layer          │
                    │  (Room SQLite + DataStore)    │
                    └───────────────────────────────┘
```

### 1.1 Unidirectional Data Flow (UDF)
Every screen follows a rigorous single-stream state contract:
- **`UiState`**: An immutable data class representing the entire UI snapshot at any point in time.
- **`UiEvent`**: A sealed interface defining all user actions (e.g., `OnTaskClick`, `OnToggleComplete`, `OnFilterChanged`).
- **`UiEffect` / Single Events**: A `Channel` / `SharedFlow` for transient actions such as triggering haptic feedback, launching a date picker, or navigating.

---

## 2. Package Structure

The codebase is organized by layer and feature under the root package `com.personal.daymark`:

```
com.personal.daymark
├── DaymarkApp.kt                  // Android Application class (initializes AppContainer)
├── MainActivity.kt                // Single Activity hosting NavHost and edge-to-edge UI
│
├── core                           // Shared utilities, base primitives, dispatchers
│   ├── common
│   │   ├── Result.kt              // Sealed interface Result<T> (Success, Error, Loading)
│   │   ├── DateUtils.kt           // Local date, time formatting, and epoch utilities
│   │   └── Extensions.kt          // Kotlin standard library & Compose helper extensions
│   ├── dispatcher
│   │   └── CoroutineDispatchers.kt// Dispatcher provider for testability (IO, Default, Main)
│   └── alarm
│       └── AlarmScheduler.kt      // Exact alarm helper for task reminders and focus timers
│
├── data                           // Concrete implementations of data sources and persistence
│   ├── local
│   │   ├── DaymarkDatabase.kt     // Room database definition and migration specs
│   │   ├── converter
│   │   │   └── Converters.kt      // TypeConverters (Priority, Recurrence, Dates)
│   │   ├── dao
│   │   │   ├── TaskDao.kt
│   │   │   ├── CategoryDao.kt
│   │   │   ├── HabitDao.kt
│   │   │   └── FocusDao.kt
│   │   └── entity
│   │       ├── TaskEntity.kt
│   │       ├── SubtaskEntity.kt
│   │       ├── CategoryEntity.kt
│   │       ├── HabitEntity.kt
│   │       ├── HabitLogEntity.kt
│   │       ├── FocusSessionEntity.kt
│   │       └── TagEntity.kt
│   ├── datastore
│   │   ├── UserPreferencesSerializer.kt
│   │   └── PreferencesManager.kt  // Protobuf / Preferences DataStore wrapper
│   └── repository
│       ├── TaskRepositoryImpl.kt
│       ├── CategoryRepositoryImpl.kt
│       ├── HabitRepositoryImpl.kt
│       ├── FocusRepositoryImpl.kt
│       └── UserPreferencesRepositoryImpl.kt
│
├── domain                         // Pure business logic and repository interfaces
│   ├── model
│   │   ├── Task.kt
│   │   ├── Subtask.kt
│   │   ├── Category.kt
│   │   ├── Habit.kt
│   │   ├── HabitStreak.kt
│   │   ├── FocusSession.kt
│   │   └── UserPreferences.kt
│   ├── repository
│   │   ├── TaskRepository.kt
│   │   ├── CategoryRepository.kt
│   │   ├── HabitRepository.kt
│   │   ├── FocusRepository.kt
│   │   └── UserPreferencesRepository.kt
│   └── usecase
│       ├── task
│       │   ├── GetTasksForTodayUseCase.kt
│       │   ├── ToggleTaskCompletionUseCase.kt
│       │   └── CreateRecurringTaskInstanceUseCase.kt
│       ├── habit
│       │   ├── CalculateHabitStreakUseCase.kt
│       │   └── ToggleHabitLogUseCase.kt
│       └── focus
│           └── LogCompletedFocusSessionUseCase.kt
│
├── di                             // Dependency Injection container
│   ├── AppContainer.kt            // Central dependency container instantiated in Application
│   └── ViewModelFactory.kt        // Generic or feature-specific ViewModelProvider.Factory
│
└── ui                             // Jetpack Compose presentation layer
    ├── theme
    │   ├── Color.kt               // Natural color tokens (Forest green, warm neutrals)
    │   ├── Type.kt                // Typography hierarchy
    │   ├── Shape.kt               // Material 3 shape tokens
    │   └── Theme.kt               // DaymarkTheme definition with dynamic & dark mode
    ├── navigation
    │   ├── DaymarkNavHost.kt      // Compose NavHost definition
    │   ├── Destinations.kt        // Sealed class / Object routes
    │   └── DaymarkBottomBar.kt    // Bottom navigation bar composable
    ├── components
    │   ├── DaymarkTopBar.kt
    │   ├── PriorityBadge.kt
    │   ├── TaskCard.kt
    │   ├── HabitHeatmap.kt
    │   ├── QuickAddBottomSheet.kt
    │   └── EmptyStateView.kt
    └── screens
        ├── today
        │   ├── TodayScreen.kt
        │   ├── TodayViewModel.kt
        │   └── TodayUiState.kt
        ├── tasks
        │   ├── TasksScreen.kt
        │   ├── TasksViewModel.kt
        │   └── TasksUiState.kt
        ├── habits
        │   ├── HabitsScreen.kt
        │   ├── HabitsViewModel.kt
        │   └── HabitsUiState.kt
        ├── focus
        │   ├── FocusScreen.kt
        │   ├── FocusViewModel.kt
        │   └── FocusUiState.kt
        ├── stats
        │   ├── StatsScreen.kt
        │   ├── StatsViewModel.kt
        │   └── StatsUiState.kt
        └── settings
            ├── SettingsScreen.kt
            ├── SettingsViewModel.kt
            └── SettingsUiState.kt
```

---

## 3. Dependency Injection Strategy

### Recommendation: Robust Manual DI (`AppContainer`)
For Daymark, a **Manual Dependency Injection container (`AppContainer`)** is strongly recommended as the primary approach, with a clean path to Hilt if required later.

**Why Manual DI is optimal for Daymark:**
1. **Zero Annotation Processing Overhead**: No KSP or Kapt annotation processing stages needed for Dagger/Hilt, reducing build and test times by over 50%.
2. **Deterministic & Transparent**: All graph instantiation happens in straightforward Kotlin code. Dependencies are clear, explicit, and easy to trace.
3. **100% Offline & Compact**: Avoids additional runtime library overhead and bytecode generation.
4. **Effortless Unit Testing**: Repository interfaces can be swapped with simple test fakes or mocks without needing custom test runners (`@HiltAndroidTest`).

### Implementation Pattern
- In `DaymarkApp.kt`, instantiate an `AppContainer`:
  ```kotlin
  class DaymarkApp : Application() {
      lateinit var container: AppContainer
          private set

      override fun onCreate() {
          super.onCreate()
          container = DefaultAppContainer(this)
      }
  }
  ```
- The `AppContainer` interface provides singleton instances:
  - Database & DAOs
  - DataStore PreferencesManager
  - Concrete Repositories
  - Dispatchers
- ViewModels obtain their dependencies via standard `ViewModelProvider.Factory`.

---

## 4. Navigation Architecture

Daymark utilizes **Jetpack Navigation Compose**. The top-level application structure is anchored by a bottom navigation bar hosting five primary destinations:

```
[ Today ]  ──  [ Tasks ]  ──  [ Habits ]  ──  [ Focus ]  ──  [ Stats ]
    │
    └── Settings (accessible via TopAppBar action icon from any screen)
```

### Destination Routes
- `Route.Today`: Primary dashboard with daily agenda, habit checks, and quick add.
- `Route.Tasks`: Full task manager with category tabs, search, and priority filtering.
- `Route.TaskDetail(taskId: Long)`: Deep-dive screen for subtasks, notes, recurrence, and reminders.
- `Route.Habits`: Habit management, streaks, and calendar views.
- `Route.Focus`: Deep work timer with Pomodoro / Stopwatch modes.
- `Route.Stats`: Visual performance charts and completion statistics.
- `Route.Settings`: Appearance, sound, notifications, backup & restore.

---

## 5. Concurrency & Data Flow Rules

1. **Room Reactive Streams**: All read queries in DAOs expose `Flow<List<T>>` or `Flow<T?>`. Room automatically re-emits when the underlying tables are mutated.
2. **Repository Abstraction**: Repositories map Room entities into pure domain models before emitting them upstream to ViewModels.
3. **StateFlow in ViewModels**: ViewModels maintain UI state via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InitialState)` to prevent unnecessary work when composables are in the background.
4. **Dispatcher Isolation**:
   - Database operations execute strictly on `Dispatchers.IO`.
   - Complex calculations (streak calculation, heatmap generation) execute on `Dispatchers.Default`.
   - UI updates and event handling remain on `Dispatchers.Main.immediate`.
