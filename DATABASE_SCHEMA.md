# Daymark - Database Schema & Data Persistence Blueprint

## 1. Overview

Daymark uses **Room (SQLite)** as its core relational persistence engine and **Jetpack Preferences DataStore** for user preferences and configuration. 

- **Database Name**: `daymark.db`
- **Schema Version**: `1`
- **Export Schema**: Enabled (`room.schemaLocation` configured in Gradle for migration validation).

---

## 2. Room Relational Entity Definitions

### 2.1 Table: `categories`
Defines user-created or default organizational buckets (e.g., *Personal*, *Work*, *Health*, *Learning*).

```sql
CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    color_hex TEXT NOT NULL,
    icon_name TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name ON categories(name);
```

#### Kotlin Entity Definition
```kotlin
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "color_hex") val colorHex: String,
    @ColumnInfo(name = "icon_name") val iconName: String?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

---

### 2.2 Table: `tasks`
Stores all top-level actionable items.

```sql
CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    category_id INTEGER,
    priority INTEGER NOT NULL DEFAULT 0,
    due_date INTEGER,
    due_time_minutes INTEGER,
    reminder_time INTEGER,
    is_completed INTEGER NOT NULL DEFAULT 0,
    completed_at INTEGER,
    is_recurring INTEGER NOT NULL DEFAULT 0,
    recurrence_rule TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS index_tasks_category_id ON tasks(category_id);
CREATE INDEX IF NOT EXISTS index_tasks_due_date ON tasks(due_date);
CREATE INDEX IF NOT EXISTS index_tasks_is_completed ON tasks(is_completed);
CREATE INDEX IF NOT EXISTS index_tasks_priority ON tasks(priority);
```

#### Kotlin Entity Definition
```kotlin
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("category_id"),
        Index("due_date"),
        Index("is_completed"),
        Index("priority")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    val priority: Int = 0, // 0 = NONE, 1 = LOW, 2 = MEDIUM, 3 = HIGH, 4 = URGENT
    @ColumnInfo(name = "due_date") val dueDate: Long? = null, // Epoch millis (start of day)
    @ColumnInfo(name = "due_time_minutes") val dueTimeMinutes: Int? = null, // 0..1439
    @ColumnInfo(name = "reminder_time") val reminderTime: Long? = null,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean = false,
    @ColumnInfo(name = "recurrence_rule") val recurrenceRule: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
```

---

### 2.3 Table: `subtasks`
Hierarchical checklist items belonging directly to a parent task.

```sql
CREATE TABLE IF NOT EXISTS subtasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    task_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    is_completed INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS index_subtasks_task_id ON subtasks(task_id);
```

#### Kotlin Entity Definition
```kotlin
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("task_id")]
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long,
    val title: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

---

### 2.4 Table: `habits`
Configurable habits tracked over days or weeks.

```sql
CREATE TABLE IF NOT EXISTS habits (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    category_id INTEGER,
    frequency_type TEXT NOT NULL, -- 'DAILY', 'WEEKLY_DAYS', 'TIMES_PER_WEEK'
    target_days_mask INTEGER NOT NULL DEFAULT 127, -- Bitmask for Mon-Sun (1111111 = 127)
    target_count_per_day INTEGER NOT NULL DEFAULT 1,
    color_hex TEXT NOT NULL,
    icon_name TEXT,
    is_archived INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS index_habits_category_id ON habits(category_id);
CREATE INDEX IF NOT EXISTS index_habits_is_archived ON habits(is_archived);
```

#### Kotlin Entity Definition
```kotlin
@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("category_id"),
        Index("is_archived")
    ]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "frequency_type") val frequencyType: String = "DAILY",
    @ColumnInfo(name = "target_days_mask") val targetDaysMask: Int = 127,
    @ColumnInfo(name = "target_count_per_day") val targetCountPerDay: Int = 1,
    @ColumnInfo(name = "color_hex") val colorHex: String,
    @ColumnInfo(name = "icon_name") val iconName: String?,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

---

### 2.5 Table: `habit_logs`
Daily execution log for habits, recording completions on a specific calendar day.

```sql
CREATE TABLE IF NOT EXISTS habit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    habit_id INTEGER NOT NULL,
    date_epoch_day INTEGER NOT NULL, -- LocalDate.toEpochDay()
    completed_count INTEGER NOT NULL DEFAULT 1,
    notes TEXT,
    logged_at INTEGER NOT NULL,
    FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS index_habit_logs_habit_id_date ON habit_logs(habit_id, date_epoch_day);
CREATE INDEX IF NOT EXISTS index_habit_logs_date ON habit_logs(date_epoch_day);
```

#### Kotlin Entity Definition
```kotlin
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habit_id", "date_epoch_day"], unique = true),
        Index("date_epoch_day")
    ]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "habit_id") val habitId: Long,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    @ColumnInfo(name = "completed_count") val completedCount: Int = 1,
    val notes: String? = null,
    @ColumnInfo(name = "logged_at") val loggedAt: Long = System.currentTimeMillis()
)
```

---

### 2.6 Table: `focus_sessions`
Records completed and in-progress Pomodoro or stopwatch deep work sessions.

```sql
CREATE TABLE IF NOT EXISTS focus_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    task_id INTEGER,
    category_id INTEGER,
    session_type TEXT NOT NULL, -- 'POMODORO', 'SHORT_BREAK', 'LONG_BREAK', 'STOPWATCH'
    duration_seconds INTEGER NOT NULL,
    started_at INTEGER NOT NULL,
    completed_at INTEGER,
    notes TEXT,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS index_focus_sessions_task_id ON focus_sessions(task_id);
CREATE INDEX IF NOT EXISTS index_focus_sessions_category_id ON focus_sessions(category_id);
CREATE INDEX IF NOT EXISTS index_focus_sessions_started_at ON focus_sessions(started_at);
```

#### Kotlin Entity Definition
```kotlin
@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("task_id"),
        Index("category_id"),
        Index("started_at")
    ]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long? = null,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "session_type") val sessionType: String,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    val notes: String? = null
)
```

---

### 2.7 Table: `tags` & `task_tag_cross_ref`
Cross-cutting organizational tags for flexible queries.

```sql
CREATE TABLE IF NOT EXISTS tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    color_hex TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name);

CREATE TABLE IF NOT EXISTS task_tag_cross_ref (
    task_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY(task_id, tag_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS index_task_tag_tag_id ON task_tag_cross_ref(tag_id);
```

---

## 3. Relational Aggregate POJOs (Room `@Relation`)

### Task with Subtasks and Category
```kotlin
data class TaskWithSubtasksAndCategory(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "task_id"
    )
    val subtasks: List<SubtaskEntity>,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)
```

### Habit with Recent Logs
```kotlin
data class HabitWithLogs(
    @Embedded val habit: HabitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "habit_id"
    )
    val logs: List<HabitLogEntity>,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)
```

---

## 4. Jetpack Preferences DataStore Schema

Non-relational user configurations, timer defaults, and UI appearance toggles are stored in `user_preferences.preferences_pb`:

| Key Name | Type | Default Value | Description |
|---|---|---|---|
| `theme_mode` | String | `"SYSTEM"` | Theme selection: `"SYSTEM"`, `"LIGHT"`, `"DARK"` |
| `accent_palette` | String | `"FOREST_GREEN"` | Palette theme: `"FOREST_GREEN"`, `"SAGE_TEAL"`, `"EARTH_WARM"`, `"SLATE"` |
| `pomodoro_work_duration` | Int | `25` | Work focus duration in minutes |
| `pomodoro_short_break` | Int | `5` | Short break duration in minutes |
| `pomodoro_long_break` | Int | `15` | Long break duration in minutes |
| `pomodoro_long_break_interval`| Int | `4` | Cycles before long break |
| `haptic_feedback_enabled` | Boolean | `true` | Haptic vibrations on task/habit completion |
| `daily_task_goal` | Int | `5` | Target task completions for daily daymark |
| `notifications_enabled` | Boolean | `true` | System reminders and timer notifications |
| `start_day_of_week` | Int | `1` | 1 = Monday (ISO default), 7 = Sunday |
