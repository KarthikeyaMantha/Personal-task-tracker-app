import React, { useState, useEffect } from 'react';
import { useLiveQuery } from 'dexie-react-hooks';
import { db, seedInitialData } from './db/db';
import { BottomNavigation, TopBar } from './components/Navigation';
import { Sidebar } from './components/Sidebar';
import { TaskSheet } from './components/TaskSheet';
import { VoiceSheet } from './components/VoiceSheet';
import { TodayScreen } from './screens/TodayScreen';
import { TasksScreen } from './screens/TasksScreen';
import { HabitsScreen } from './screens/HabitsScreen';
import { CalendarScreen } from './screens/CalendarScreen';
import { FocusScreen } from './screens/FocusScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { Share, PlusSquare } from 'lucide-react';
import type { Task, Habit } from './types';

export function App() {
  const [currentTab, setCurrentTab] = useState<'today' | 'tasks' | 'calendar' | 'habits'>('today');
  const [activeScreen, setActiveScreen] = useState<'main' | 'focus' | 'settings'>('main');
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isTaskSheetOpen, setIsTaskSheetOpen] = useState(false);
  const [taskToEdit, setTaskToEdit] = useState<Task | null>(null);
  const [isVoiceOpen, setIsVoiceOpen] = useState(false);
  const [theme, setTheme] = useState<'system' | 'light' | 'dark'>('dark');
  const [showIosPrompt, setShowIosPrompt] = useState(false);

  // Seed on startup
  useEffect(() => {
    seedInitialData();

    // Check if running on iOS Safari in browser (not standalone PWA)
    const isIos = /iPad|iPhone|iPod/.test(navigator.userAgent) && !(window as any).MSStream;
    const isStandalone = window.matchMedia('(display-mode: standalone)').matches || (navigator as any).standalone;
    if (isIos && !isStandalone) {
      setShowIosPrompt(true);
    }
  }, []);

  // Live queries
  const tasks = useLiveQuery(() => db.tasks.toArray(), []) || [];
  const habits = useLiveQuery(() => db.habits.toArray(), []) || [];
  const categories = useLiveQuery(() => db.categories.toArray(), []) || [];

  // Theme application
  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'dark' || (theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
      root.classList.add('dark');
      document.body.style.backgroundColor = '#121214';
      document.body.style.color = '#FFFFFF';
    } else {
      root.classList.remove('dark');
      document.body.style.backgroundColor = '#F4F4F5';
      document.body.style.color = '#121214';
    }
  }, [theme]);

  // Task actions
  const handleSaveTask = async (taskData: Omit<Task, 'id' | 'createdAt'> & { id?: number }) => {
    if (taskData.id) {
      await db.tasks.update(taskData.id, {
        ...taskData,
      });
    } else {
      await db.tasks.add({
        ...taskData,
        createdAt: Date.now(),
      });
    }
  };

  const handleDeleteTask = async (id: number) => {
    await db.tasks.delete(id);
  };

  const handleToggleTask = async (task: Task) => {
    if (!task.id) return;
    await db.tasks.update(task.id, {
      isCompleted: !task.isCompleted,
    });
  };

  const handleSnoozeTask = async (task: Task) => {
    if (!task.id) return;
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(0, 0, 0, 0);
    await db.tasks.update(task.id, {
      dueDate: tomorrow.getTime(),
    });
  };

  // Habit actions
  const handleToggleHabit = async (habit: Habit) => {
    if (!habit.id) return;
    const newStreak = habit.currentStreak + 1;
    await db.habits.update(habit.id, {
      currentStreak: newStreak,
      longestStreak: Math.max(habit.longestStreak, newStreak),
    });
  };

  const handleAddHabit = async (habitData: Omit<Habit, 'id' | 'createdAt' | 'currentStreak' | 'longestStreak'>) => {
    await db.habits.add({
      ...habitData,
      currentStreak: 0,
      longestStreak: 0,
      createdAt: Date.now(),
    });
  };

  const handleDeleteHabit = async (id: number) => {
    await db.habits.delete(id);
  };

  return (
    <div className="min-h-screen flex flex-col font-sans">
      {/* Top Header */}
      {activeScreen === 'main' && (
        <TopBar
          title={currentTab.charAt(0).toUpperCase() + currentTab.slice(1)}
          onOpenSidebar={() => setIsSidebarOpen(true)}
          onOpenVoice={() => setIsVoiceOpen(true)}
          onOpenFocus={() => setActiveScreen('focus')}
          onOpenInsights={() => setActiveScreen('settings')}
          onOpenWrapUp={() => setActiveScreen('focus')}
        />
      )}

      {/* Main Content Area */}
      <main className="flex-1 max-w-lg w-full mx-auto px-4 pt-4">
        {/* iOS Add-to-Home-Screen Guidance Pill */}
        {showIosPrompt && (
          <div className="mb-4 bg-coral/10 border border-coral/30 rounded-2xl p-3.5 text-xs text-zinc-300 flex items-center justify-between shadow-sm">
            <div className="flex items-center space-x-2.5">
              <Share className="w-4 h-4 text-coral flex-shrink-0" />
              <span>
                To install on iPhone: Tap <strong className="text-white">Share</strong> then <strong className="text-white">Add to Home Screen</strong> <PlusSquare className="inline w-3.5 h-3.5 ml-0.5 text-coral" />
              </span>
            </div>
            <button
              onClick={() => setShowIosPrompt(false)}
              className="text-zinc-500 hover:text-white p-1 text-xs font-bold"
            >
              ✕
            </button>
          </div>
        )}

        {activeScreen === 'focus' ? (
          <FocusScreen onBack={() => setActiveScreen('main')} />
        ) : activeScreen === 'settings' ? (
          <SettingsScreen
            onBack={() => setActiveScreen('main')}
            theme={theme}
            onThemeChange={setTheme}
          />
        ) : currentTab === 'today' ? (
          <TodayScreen
            tasks={tasks}
            habits={habits}
            categories={categories}
            onToggleTask={handleToggleTask}
            onEditTask={(t) => {
              setTaskToEdit(t);
              setIsTaskSheetOpen(true);
            }}
            onDeleteTask={handleDeleteTask}
            onSnoozeTask={handleSnoozeTask}
            onOpenNewTask={() => {
              setTaskToEdit(null);
              setIsTaskSheetOpen(true);
            }}
            onOpenVoice={() => setIsVoiceOpen(true)}
            onOpenFocus={() => setActiveScreen('focus')}
          />
        ) : currentTab === 'tasks' ? (
          <TasksScreen
            tasks={tasks}
            categories={categories}
            onToggleTask={handleToggleTask}
            onEditTask={(t) => {
              setTaskToEdit(t);
              setIsTaskSheetOpen(true);
            }}
            onDeleteTask={handleDeleteTask}
            onSnoozeTask={handleSnoozeTask}
            onOpenNewTask={() => {
              setTaskToEdit(null);
              setIsTaskSheetOpen(true);
            }}
          />
        ) : currentTab === 'habits' ? (
          <HabitsScreen
            habits={habits}
            onToggleHabit={handleToggleHabit}
            onAddHabit={handleAddHabit}
            onDeleteHabit={handleDeleteHabit}
          />
        ) : (
          <CalendarScreen
            tasks={tasks}
            categories={categories}
            onToggleTask={handleToggleTask}
            onEditTask={(t) => {
              setTaskToEdit(t);
              setIsTaskSheetOpen(true);
            }}
            onDeleteTask={handleDeleteTask}
            onOpenNewTaskForDate={(d) => {
              setTaskToEdit(null);
              setIsTaskSheetOpen(true);
            }}
          />
        )}
      </main>

      {/* Bottom Navigation */}
      {activeScreen === 'main' && (
        <BottomNavigation
          currentTab={currentTab}
          onTabChange={(t) => {
            setCurrentTab(t);
            setActiveScreen('main');
          }}
        />
      )}

      {/* Sidebar Drawer */}
      <Sidebar
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
        onNavigateToSettings={() => {
          setActiveScreen('settings');
        }}
        onNavigateToInsights={() => {
          setActiveScreen('settings');
        }}
        onNavigateToWeeklyReview={() => {
          setActiveScreen('settings');
        }}
        onNavigateToTemplates={() => {
          setActiveScreen('settings');
        }}
      />

      {/* New/Edit Task Modal Sheet */}
      <TaskSheet
        isOpen={isTaskSheetOpen}
        onClose={() => {
          setIsTaskSheetOpen(false);
          setTaskToEdit(null);
        }}
        onSave={handleSaveTask}
        onDelete={handleDeleteTask}
        taskToEdit={taskToEdit}
        categories={categories}
      />

      {/* Voice Action Modal Sheet */}
      <VoiceSheet
        isOpen={isVoiceOpen}
        onClose={() => setIsVoiceOpen(false)}
        onConfirmTask={handleSaveTask}
      />
    </div>
  );
}

export default App;

