# Daymark - Product & Technical Specification

## 1. Executive Summary

**Daymark** is a modern, native offline-first Android personal productivity application engineered for focus, daily intention, and personal data sovereignty. 

The application unifies four core pillars of individual productivity:
1. **Task & Project Management** (hierarchical tasks, subtasks, categories, priorities, due dates, recurrences).
2. **Daily Planning ("Today" Daymark View)** (time-aligned agenda, quick-capture scratchpad, daily check-in/review).
3. **Habit Building & Streak Tracking** (flexible recurrence, completion history, calendar heatmaps).
4. **Deep Work & Focus Timer** (integrated Pomodoro & stopwatch timer directly linked to tasks and categories).

---

## 2. Privacy & Offline-First Principles

Daymark operates under a strict **Zero-Cloud, Zero-Tracking** architecture:

- **No Internet Access**: The `android.permission.INTERNET` permission is completely omitted from `AndroidManifest.xml`. The operating system guarantees that the app cannot communicate with external servers.
- **Local Sovereignty**: All user data resides exclusively on the physical device in an encrypted or sandboxed SQLite database via Android Room and Encrypted/Preferences DataStore.
- **No Accounts or Logins**: The application has no sign-up screens, passwords, or authentication hurdles. Instant utility upon first launch.
- **Zero Third-Party SDKs**: No analytics libraries (Firebase, Mixpanel, Segment), no crash reporters sending data over the wire, no advertising SDKs.
- **Complete Data Portability**: Users can export their full dataset to JSON or CSV via Android's native Storage Access Framework (SAF) and restore it on any device at any time.

---

## 3. Core Feature Requirements

### 3.1 Task & Subtask Management
- **Task Attributes**: Title, optional rich description/notes, category/project link, priority level, due date, due time, reminder timestamp, completion status, completion timestamp, recurring schedule rule, and tags.
- **Priority Hierarchy**:
  - `URGENT` (Level 4): High visibility badge, subtle crimson accent.
  - `HIGH` (Level 3): Warm amber accent.
  - `MEDIUM` (Level 2): Forest sage accent.
  - `LOW` (Level 1): Muted slate accent.
  - `NONE` (Level 0): Neutral surface tone.
- **Subtasks / Checklists**:
  - Reorderable checklist items within tasks.
  - Visual completion progress indicator (e.g., `3/5 subtasks complete`).
  - Automatic task completion option when all subtasks are finished.
- **Categories & Tags**:
  - Custom user-created categories (e.g., *Work*, *Personal*, *Health*, *Creative*).
  - Hex color coding and curated Material icons.
  - Multi-tag assignment for flexible cross-cutting queries.
- **Recurrence Engine**:
  - Daily, Weekdays, Weekly, Monthly, or Custom interval (e.g., every 3 days).
  - Configurable recurring logic: generates a new instance upon completion or repeats on a strict calendar schedule.

### 3.2 "Today" Dashboard & Daily Daymark
- **Daily Focus Hub**: Curated view of tasks scheduled for today, overdue tasks requiring rescheduling, and daily habit commitments.
- **Quick-Add Sheet**: Persistent or bottom-navigation accessible modal for rapid idea capture in < 2 seconds.
- **Daily Reflection / Daymark**: End-of-day summary presenting tasks completed, habits logged, and focus minutes recorded.

### 3.3 Habit Tracking
- **Habit Definitions**: Habit title, description, category, target frequency (e.g., daily, specific days of week, X times per week), target counter (e.g., 8 glasses of water, 1 meditation session).
- **Streak Engine**:
  - Automatic calculation of current streak, longest streak, and completion rate.
  - Grace period / streak freeze toggle to encourage persistence without punishment.
- **Visual Progress**:
  - Weekly inline day-bubbles (`M T W T F S S`) with instant toggle.
  - Monthly/annual visual contribution heatmap.

### 3.4 Deep Work & Focus Timer
- **Modes**:
  - **Pomodoro Mode**: Configurable intervals (default 25 min work, 5 min short break, 15 min long break after 4 cycles).
  - **Stopwatch / Flow Mode**: Open-ended count-up timer for uninterrupted deep work.
- **Contextual Linking**: Every focus session can be tagged to a specific task or high-level category.
- **Local Notifications & Alarms**:
  - Exact alarms via Android `AlarmManager` for timer completion.
  - Persistent foreground notification with live countdown and pause/skip actions.
- **Session History**: Automatic logging of completed session duration, timestamp, and notes.

### 3.5 Local Analytics & Insights
- **Task Trends**: Weekly and monthly task completion curves, velocity, and completion by category.
- **Habit Consistency**: Visual adherence rate over 7, 30, and 90 days.
- **Focus Distribution**: Pie / bar breakdown of time invested per category and peak productivity hours.

### 3.6 Settings & Preferences
- Dynamic Material 3 theme switching (Light, Dark, System default).
- Palette accents: Forest Green (Default), Sage Teal, Earth Warmth, Deep Slate.
- Pomodoro timing configuration.
- Notification sound & vibration toggle.
- Data backup (SAF export to JSON/ZIP) and data restore (JSON file validation and import).

---

## 4. UI/UX Design Direction

Daymark rejects the cluttered, high-saturation, hyperactive aesthetics of modern commercial software in favor of an **editorial, serene, tactile, and grounded** experience.

### 4.1 Aesthetic Philosophy
- **Tactile & Natural**: Warm light stone backgrounds (`#FBF9F5` / `#F5F2EB`) paired with deep forest greens (`#1B4332`), sage teals (`#40916C`), and earthy charcoal text (`#191C1A`).
- **Restraint Over Noise**: No harsh neon colors, no artificial glassmorphism or distracting blur filters.
- **Material 3 Principles**: Semantic color roles (`primary`, `onPrimary`, `surfaceContainer`, `surfaceContainerHigh`, `outlineVariant`), tonal elevation, and smooth container transforms.
- **Typographic Hierarchy**:
  - Headlines & Titles: Clean geometric/editorial sans-serif with strong typographic presence.
  - Body & Metadata: High-legibility sans-serif with careful line-height and letter-spacing.
- **Micro-Interactions & Haptics**: Subtle tactile clicks on task completion, gentle spring physics on sheet dismissals, and fluid state transitions.

---

## 5. Non-Functional Requirements & Performance Targets

1. **Cold Start Time**: Under 500ms on mid-range Android devices.
2. **Frame Rate**: Continuous 60/120fps scrolling with zero jank in Jetpack Compose lazy lists.
3. **Database Footprint**: SQLite database size optimized via normalized schemas and targeted indices (< 15MB for 10,000 tasks and logs).
4. **Battery Efficiency**: Zero background polling or unnecessary wake locks; exact alarms used only for active timers or user-scheduled reminders.
5. **Memory Consumption**: Low steady-state RAM usage (< 60MB).
6. **Platform Compatibility**: Minimum SDK: API 26 (Android 8.0 Oreo), Target SDK: API 34/35 (Android 14/15).
