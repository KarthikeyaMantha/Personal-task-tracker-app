# Daymark - 15-Phase Master Implementation Plan

This roadmap outlines the structured, incremental delivery sequence for Daymark. Each phase delivers a robust, testable, and verified layer of the application.

---

### Phase 1: Workspace Inspection & Project Blueprint *(Current Phase)*
- Inspect the developer environment (workspace, Android SDK, JDK, Gradle).
- Author architectural specifications, schema blueprints, and project rules.
- **Deliverables**: `PROJECT_SPEC.md`, `ARCHITECTURE.md`, `DATABASE_SCHEMA.md`, `IMPLEMENTATION_PLAN.md`, `QWEN.md`.

---

### Phase 2: Android Project Foundation & Build System
- Initialize the root and app-level Gradle build configurations using Kotlin DSL (`build.gradle.kts`).
- Configure Version Catalog (`gradle/libs.versions.toml`) with Jetpack Compose, Room, DataStore, Coroutines, and Lifecycle dependencies.
- Setup `AndroidManifest.xml` (offline configuration, no INTERNET permission) and base `DaymarkApp` Application class.
- Verify Gradle sync and basic build compilation.

---

### Phase 3: Design System & Material 3 Theming
- Implement color palettes (Forest Green primary, Sage accents, warm light stone background, deep charcoal dark mode).
- Configure typography scales, shapes, and tonal surface elevation.
- Implement `DaymarkTheme` with dynamic color toggling, light/dark modes, and preview fixtures.

---

### Phase 4: Room Database & Persistence Layer
- Define Room Entities (`TaskEntity`, `SubtaskEntity`, `CategoryEntity`, `HabitEntity`, `HabitLogEntity`, `FocusSessionEntity`, `TagEntity`).
- Implement DAOs with reactive `Flow` query patterns and suspend write functions.
- Configure `DaymarkDatabase`, Room TypeConverters, and migration testing harness.

---

### Phase 5: Preferences DataStore & Settings Repository
- Create DataStore preferences wrapper for theme, accent palette, Pomodoro timers, and haptics.
- Implement `UserPreferencesRepository` with reactive `Flow<UserPreferences>`.
- Unit tests for preference persistence and default values.

---

### Phase 6: Domain Repositories & Business Logic
- Create clean domain models and repository interfaces.
- Implement repository classes (`TaskRepositoryImpl`, `HabitRepositoryImpl`, `FocusRepositoryImpl`).
- Implement core use cases (e.g., recurring task calculation, habit streak computation engine).

---

### Phase 7: Reusable Compose Design Components
- Build foundation UI components: `DaymarkTopBar`, `TaskCard`, `PriorityBadge`, `HabitHeatmap`, `TagChip`, `EmptyStateView`.
- Build the `QuickAddBottomSheet` with category, priority, and date pickers.
- Integrate tactile haptic feedback helpers.

---

### Phase 8: Navigation Architecture & App Shell
- Configure Jetpack Navigation Compose with sealed destination routes.
- Implement `DaymarkBottomBar` for 5 top-level destinations (Today, Tasks, Habits, Focus, Stats).
- Integrate top-app-bar actions for global Search and Settings modal.

---

### Phase 9: Task Management Feature
- Task list screen with interactive category filter tabs, priority filtering, and search.
- Task detail screen supporting subtask checklists, recurrence rules, and notes.
- Swipe gestures for quick completion and deletion.

---

### Phase 10: "Today" Dashboard & Daily Daymark
- Implement the "Today" screen featuring overdue warnings, today's schedule, and daily habit commitments.
- Daily progress celebration card and reflection summary.
- Global quick-capture integration.

---

### Phase 11: Habit Tracking Feature
- Habit dashboard with streak indicators and weekly day-bubble toggle grids.
- Habit creation and editing flow with custom frequency rules.
- Visual completion heatmap (GitHub-style contribution grid for habits).

---

### Phase 12: Focus Timer & Deep Work Feature
- Pomodoro timer with configurable work/break cycles and circular progress ring.
- Stopwatch flow mode for open-ended sessions.
- Link timer directly to tasks or categories, and record completed sessions to Room.

---

### Phase 13: Local Analytics & Insights Feature
- Offline statistics screen: task velocity, completion rate by category.
- Habit consistency trends over 7, 30, and 90 days.
- Focus time distribution charts without external web dependencies.

---

### Phase 14: Data Export, Import & Backup
- JSON data export engine utilizing Android Storage Access Framework (SAF).
- JSON validation and database restore / merge logic.
- Total offline backup and restore guarantee for complete user ownership.

---

### Phase 15: Polish, Performance & Production Readiness
- Edge-to-edge UI audit (`WindowInsets`, system bars contrast).
- Accessibility review (content descriptions, min touch targets 48dp, contrast compliance).
- Profiling of cold startup, lazy list scroll performance, and memory consumption.
- Final documentation and build audit.
