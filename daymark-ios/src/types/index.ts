export interface Task {
  id?: number;
  title: string;
  description?: string;
  isCompleted: boolean;
  dueDate?: number; // timestamp ms
  dueTime?: number; // minutes from midnight (e.g. 1080 = 6pm)
  priority?: number; // 1 = Low, 2 = Medium, 3 = High, 4 = Urgent
  categoryId?: number;
  recurrenceRule?: string; // e.g. "DAILY", "WEEKDAYS"
  createdAt: number;
}

export interface Habit {
  id?: number;
  name: string;
  frequency: 'DAILY' | 'SPECIFIC_DAYS';
  targetWeekdays?: number; // bitmask: 127 = all, 31 = Mon-Fri
  targetDurationMinutes?: number;
  routineTimeOfDay?: 'MORNING' | 'AFTERNOON' | 'EVENING' | 'ANYTIME';
  currentStreak: number;
  longestStreak: number;
  createdAt: number;
}

export interface HabitLog {
  id?: number;
  habitId: number;
  date: string; // YYYY-MM-DD
  isCompleted: boolean;
  amount?: number;
}

export interface Category {
  id?: number;
  name: string;
  color: string;
}

export interface ParsedTaskResult {
  cleanTitle: string;
  categoryName?: string;
  priority?: number;
  dueDate?: number;
  dueTimeMinutes?: number;
  tokens: string[];
}

