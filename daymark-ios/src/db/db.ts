import Dexie, { type Table } from 'dexie';
import type { Task, Habit, HabitLog, Category } from '../types';

export class DaymarkDatabase extends Dexie {
  tasks!: Table<Task, number>;
  habits!: Table<Habit, number>;
  habitLogs!: Table<HabitLog, number>;
  categories!: Table<Category, number>;

  constructor() {
    super('DaymarkDB');
    this.version(1).stores({
      tasks: '++id, title, isCompleted, dueDate, priority, categoryId, createdAt',
      habits: '++id, name, routineTimeOfDay, createdAt',
      habitLogs: '++id, habitId, date, isCompleted, [habitId+date]',
      categories: '++id, name'
    });
  }
}

export const db = new DaymarkDatabase();

// Seed initial default data if empty
export async function seedInitialData() {
  const catCount = await db.categories.count();
  if (catCount === 0) {
    await db.categories.bulkAdd([
      { name: 'Personal', color: '#10B981' },
      { name: 'Work', color: '#3B82F6' },
      { name: 'Health', color: '#F59E0B' },
      { name: 'Study', color: '#8B5CF6' }
    ]);
  }

  const habitCount = await db.habits.count();
  if (habitCount === 0) {
    await db.habits.bulkAdd([
      {
        name: 'Morning Hydration',
        frequency: 'DAILY',
        routineTimeOfDay: 'MORNING',
        currentStreak: 1,
        longestStreak: 3,
        createdAt: Date.now()
      },
      {
        name: '20m Focused Reading',
        frequency: 'DAILY',
        targetDurationMinutes: 20,
        routineTimeOfDay: 'EVENING',
        currentStreak: 2,
        longestStreak: 5,
        createdAt: Date.now()
      }
    ]);
  }
}

