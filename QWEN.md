# Daymark - Project Context & Operational Directives

## 1. App Identity

- **Application Name**: Daymark
- **Package Name**: `com.personal.daymark`
- **Platform**: Native Android (Min SDK 26, Target SDK 34/35)
- **Primary Tech Stack**: Kotlin, Jetpack Compose, Material 3, Clean Architecture + MVVM, Room (SQLite), Preferences DataStore, Kotlin Coroutines & Flow.
- **Core Mission**: Provide a serene, tactile, distraction-free productivity environment uniting task management, daily planning, habit tracking, and deep work focus.

---

## 2. Non-Negotiable Core Architectural Rules

1. **Strictly Offline-Only (Zero Network)**:
   - `android.permission.INTERNET` must NEVER be declared in `AndroidManifest.xml`.
   - No network calls, remote APIs, cloud sync, or server dependencies.
   - All persistence is strictly local via Room SQLite and DataStore.

2. **Absolute Privacy & Data Sovereignty**:
   - Zero telemetry, analytics, or crash reporters transmitting data outside the device.
   - No accounts, logins, or authentication barriers.
   - Data backup and migration is handled exclusively via offline file exports (JSON) using the Android Storage Access Framework (SAF).

3. **Zero Monetization Noise**:
   - No ads, paywalls, promotional banners, or subscription lockouts.

4. **Curated Visual Craft (Warm Editorial & Calm Utility)**:
   - Palette: Warm light neutral ground (`#FBF9F5` / `#F5F2EB`), Forest Green primary (`#1B4332`), Sage accents (`#40916C`), deep charcoal text (`#191C1A`), and slate dark mode (`#121513`).
   - Strictly NO neon colors, NO jarring glassmorphism blur layers.
   - Material 3 design system with tactile haptics and fluid micro-interactions.

5. **Clean Unidirectional Data Flow (UDF)**:
   - Immutable `UiState` exposed via `StateFlow`.
   - Distinct `UiEvent` sealed hierarchies.
   - Separation of concerns: UI composables observe state; ViewModels interact with domain repositories; data layer handles Room and DataStore operations on `Dispatchers.IO`.

---

## 3. Current Project Phase Status

- **Phase 1: Workspace Inspection & Project Blueprint**: COMPLETE
- **Phase 2: Android Project Foundation & Build System**: COMPLETE
- **Phase 3: Design System and Navigation**: COMPLETE
- **Phase 4: Database and Local Persistence**: COMPLETE
- **Phase 5: Core Task Management**: COMPLETE
  - ViewModels implemented: `TaskListViewModel` and `AddEditTaskViewModel` with factory providers.
  - Fast task entry implemented: only "Title" is required to save a task; optional due date, priority, notes, and category.
  - CRUD operations implemented: create, edit, toggle completion, and delete.
  - Undo deletion flow implemented using Compose `Snackbar` with an "Undo" action.
  - `SwipeableTaskRow` implemented with `SwipeToDismissBox`: swipe-to-complete (start-to-end) and swipe-to-delete (end-to-start).
  - Interactive `TasksScreen` with category chips, expandable search, empty state, and FAB for `AddEditTaskBottomSheet`.
  - Local unit tests implemented and passing (15/15 unit tests pass).
  - Clean APK assembled (`./gradlew assembleDebug` passed).
- **Phase 6: Dashboard and Task Views**: COMPLETE
  - Attractive "Today" screen (default landing screen) with dynamic greeting (morning/afternoon/evening), current date banner, overdue warnings, today's schedule, reminders placeholder, and calming empty states.
  - Compact completion progress indicator (`DaymarkProgressCard`) calculating real-time completion ratio based on actual database state (`completedToday / (totalDueToday + overdue)`).
  - 5 Dedicated Task Views with filter tabs & badge counts:
    - **All**: All active/pending tasks
    - **Today**: Tasks due today
    - **Inbox**: Unscheduled tasks (`dueDate == null`)
    - **Upcoming**: Tasks due in the future (sorted by date)
    - **Overdue**: Incomplete past-due tasks (retained until completed/rescheduled, never disappears)
    - **Completed**: Historical completed tasks
  - Comprehensive sorting options: Due Date, Priority (Highest first), Creation Date (Newest first), Alphabetical (A-Z).
  - Category filtering integrated across all views.
  - Unit tests updated and expanded (22/22 unit tests passing).
  - `./gradlew assembleDebug` successfully verified.
- **Phase 7: Reminder and Notification Engine**: COMPLETE
  - Notification Channels initialized in `DaymarkApplication`: "Task Reminders" (`IMPORTANCE_HIGH` with sound & vibration) and "Focus Timer" (`IMPORTANCE_DEFAULT`).
  - Android 13+ (API 33+) runtime permission flow for `POST_NOTIFICATIONS` handled on startup in `MainActivity`.
  - Manifest permissions configured: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `VIBRATE`.
  - `AlarmManagerReminderScheduler` implemented using `AlarmManager.setExactAndAllowWhileIdle()` with fallback to inexact when restricted.
  - BroadcastReceivers implemented and registered:
    - `TaskReminderReceiver`: verifies task state in Room DB before triggering notification; configures notification with tap intent, "Mark Complete", "Snooze 10m", and "Snooze 30m" actions.
    - `NotificationActionReceiver`: handles "Mark Complete" (updates Room DB directly, cancels notification) and "Snooze" (reschedules alarm for 10m/30m).
    - `BootCompletedReceiver`: reschedules all pending future alarms after reboot/package replace.
  - UI Integration: `AddEditTaskBottomSheet` expanded with custom Date Picker (`DatePickerDialog`) and Time Picker (`TimePickerDialog`), quick reminder chips (Morning, Afternoon, Evening), and real-time confirmation pill.
  - System Reliability & Battery Optimization: `SystemReliabilityBanner` added to `TodayScreen` and dedicated "Reminders & Reliability" section in `SettingsScreen` with one-tap deep links to system settings for notifications, exact alarms, and battery optimizations.
  - Unit tests expanded and passing: 33/33 unit tests pass (`./gradlew test`).
  - APK cleanly assembled (`./gradlew assembleDebug` passed).
- **Phase 8: Recurring Tasks**: COMPLETE
  - Data model and Room schema: Added `series_id` column and index to `TaskEntity`, bumped database version to 2 with `MIGRATION_1_2` SQLite migration.
  - Recurrence engine: Created `RecurrenceRule` (Daily, Weekdays, Weekly with customizable day-of-week selection, Monthly, Yearly) and `RecurrenceHelper` for robust calendar math and month-end boundary handling.
  - Completion & Duplicate Prevention: Centralized `completeTaskAndGenerateNext` in `TaskRepository` ensuring idempotent execution, zero duplicate instances, and automatic next occurrence creation with notification reminder scheduling.
  - Action Receiver integration: Notification "Mark Complete" invokes `completeTaskAndGenerateNext` and schedules next recurring reminder.
  - Deleting & Editing dialogs:
    - Edit dialog ("This task only" detaches instance vs. "Entire series" updates all active tasks in series).
    - Delete dialog ("This task only" removes occurrence while advancing series vs. "Entire series" removes all series instances and cancels notifications).
  - UI Integration: `AddEditTaskBottomSheet` expanded with repeat options and specific weekday selector chips; `TaskRow` and `SwipeableTaskRow` display recurrence indicators (`🔁 Repeats Daily`, etc.).
  - Testing & Verification: 42/42 unit tests pass (`./gradlew test`), `./gradlew assembleDebug` builds cleanly with exit code 0.
- **Next Phase**: **Phase 9: Categories, Projects, and Subtasks**


