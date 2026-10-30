import React, { useState, useEffect } from 'react';
import { Plus, Flame, Check, Droplets, Clock, Trash2, X } from 'lucide-react';
import type { Habit, HabitLog } from '../types';

interface HabitsScreenProps {
  habits: Habit[];
  habitLogs?: HabitLog[];
  onToggleHabit: (habit: Habit) => void;
  onAddHabit: (habitData: Omit<Habit, 'id' | 'createdAt' | 'currentStreak' | 'longestStreak'>) => Promise<void>;
  onDeleteHabit: (id: number) => Promise<void>;
}

export const HabitsScreen: React.FC<HabitsScreenProps> = ({
  habits,
  habitLogs = [],
  onToggleHabit,
  onAddHabit,
  onDeleteHabit,
}) => {
  const [selectedRoutine, setSelectedRoutine] = useState<string | null>(null);
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [newHabitName, setNewHabitName] = useState('');
  const [newRoutine, setNewRoutine] = useState<'MORNING' | 'AFTERNOON' | 'EVENING' | 'ANYTIME'>('MORNING');

  // Close modal on Escape
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isAddOpen) {
        setIsAddOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isAddOpen]);

  const routines = [
    { id: null, label: 'All Routines' },
    { id: 'MORNING', label: '🌅 Morning' },
    { id: 'AFTERNOON', label: '☀️ Afternoon' },
    { id: 'EVENING', label: '🌙 Evening' },
    { id: 'ANYTIME', label: '⚡ Anytime' },
  ];

  const todayStr = new Date().toISOString().split('T')[0];

  const filteredHabits = habits.filter((h) => {
    if (!selectedRoutine) return true;
    return h.routineTimeOfDay === selectedRoutine;
  });

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newHabitName.trim()) return;
    await onAddHabit({
      name: newHabitName.trim(),
      frequency: 'DAILY',
      routineTimeOfDay: newRoutine,
    });
    setNewHabitName('');
    setIsAddOpen(false);
  };

  return (
    <div className="space-y-4 pb-28">
      {/* Horizontal Scrollable Routine Filter Chips */}
      <div className="flex gap-2 overflow-x-auto no-scrollbar py-1" role="radiogroup" aria-label="Habit routines">
        {routines.map((r) => {
          const isSelected = selectedRoutine === r.id;
          return (
            <button
              key={r.label}
              type="button"
              role="radio"
              aria-checked={isSelected}
              onClick={() => setSelectedRoutine(r.id)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all focus:outline-none focus:ring-1 focus:ring-coral ${
                isSelected
                  ? 'bg-coral text-white shadow-md shadow-coral/30'
                  : 'bg-[#18181B] text-zinc-300 border border-[#27272A] hover:bg-zinc-800'
              }`}
            >
              {r.label}
            </button>
          );
        })}
      </div>

      {/* Habit Cards */}
      {filteredHabits.length === 0 ? (
        <div className="bg-[#18181B] border border-[#27272A] rounded-3xl p-8 text-center mt-6 space-y-3">
          <p className="text-sm font-semibold text-zinc-300">No habits in this routine</p>
          <p className="text-xs text-zinc-400">
            Build mindful consistency by creating your daily rituals.
          </p>
          <button
            type="button"
            onClick={() => setIsAddOpen(true)}
            className="px-4 py-2 rounded-xl text-xs font-bold bg-coral text-white shadow-md shadow-coral/30 inline-flex items-center space-x-1.5 hover:bg-coral-light active:scale-95 transition"
          >
            <Plus className="w-4 h-4 stroke-[3]" aria-hidden="true" />
            <span>Create First Habit</span>
          </button>
        </div>
      ) : (
        <div className="space-y-2.5">
          {filteredHabits.map((habit) => {
            const isCompletedToday = habitLogs.some(
              (l) => l.habitId === habit.id && l.date === todayStr && l.isCompleted
            );

            return (
              <div
                key={habit.id}
                className="flex items-center justify-between p-4 rounded-2xl bg-[#1A1A1E] border border-[#27272A] shadow-sm hover:border-zinc-700 transition"
              >
                <div className="flex items-center space-x-3.5">
                  <button
                    type="button"
                    role="checkbox"
                    aria-checked={isCompletedToday}
                    aria-label={`Mark habit "${habit.name}" as ${isCompletedToday ? 'incomplete' : 'complete'}`}
                    onClick={() => onToggleHabit(habit)}
                    className={`w-8 h-8 rounded-full border-2 flex items-center justify-center transition active:scale-90 focus:outline-none focus:ring-2 focus:ring-coral/70 ${
                      isCompletedToday
                        ? 'bg-coral border-coral shadow-sm shadow-coral/30'
                        : 'border-zinc-500 hover:border-coral bg-transparent'
                    }`}
                  >
                    <Check
                      className={`w-4 h-4 transition ${
                        isCompletedToday ? 'text-white stroke-[3]' : 'text-transparent hover:text-coral'
                      }`}
                      aria-hidden="true"
                    />
                  </button>
                  <div>
                    <h3 className={`text-sm font-bold ${isCompletedToday ? 'text-zinc-300' : 'text-white'}`}>
                      {habit.name}
                    </h3>
                    <div className="flex items-center space-x-2 mt-1">
                      <span className="flex items-center space-x-1 text-xs font-semibold text-amber-400">
                        <Flame className="w-3.5 h-3.5 fill-amber-400" aria-hidden="true" />
                        <span>{habit.currentStreak} day streak</span>
                      </span>
                      <span className="text-zinc-600" aria-hidden="true">•</span>
                      <span className="text-[11px] text-zinc-400 capitalize">
                        {habit.routineTimeOfDay?.toLowerCase() || 'daily'}
                      </span>
                    </div>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => habit.id && onDeleteHabit(habit.id)}
                  aria-label={`Delete habit "${habit.name}"`}
                  className="p-2 rounded-lg text-zinc-400 hover:text-rose-400 hover:bg-rose-500/10 transition focus:outline-none focus:ring-1 focus:ring-rose-500"
                  title="Delete habit"
                >
                  <Trash2 className="w-4 h-4" aria-hidden="true" />
                </button>
              </div>
            );
          })}
        </div>
      )}

      {/* Floating Action Button (+) */}
      <button
        type="button"
        onClick={() => setIsAddOpen(true)}
        aria-label="Add new habit"
        className="fixed right-5 fab-bottom-safe z-30 w-14 h-14 rounded-full bg-coral text-white shadow-xl shadow-coral/40 flex items-center justify-center hover:bg-coral-light active:scale-95 transition-all focus:outline-none focus:ring-2 focus:ring-coral"
      >
        <Plus className="w-7 h-7 stroke-[2.8]" aria-hidden="true" />
      </button>

      {/* Add Habit Modal */}
      {isAddOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="new-habit-title"
          className="fixed inset-0 z-50 flex items-end justify-center p-4"
        >
          <div className="fixed inset-0 bg-black/75 backdrop-blur-sm" onClick={() => setIsAddOpen(false)} aria-hidden="true" />
          <div className="relative w-full max-w-sm bg-[#202024] border border-zinc-700 rounded-3xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 id="new-habit-title" className="text-base font-bold text-white">New Habit</h3>
              <button
                type="button"
                onClick={() => setIsAddOpen(false)}
                aria-label="Close dialog"
                className="p-1 text-zinc-400 hover:text-white"
              >
                <X className="w-5 h-5" aria-hidden="true" />
              </button>
            </div>
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label htmlFor="habit-name-input" className="sr-only">Habit name</label>
                <input
                  id="habit-name-input"
                  type="text"
                  autoFocus
                  placeholder="e.g. Morning 10-minute stretch"
                  value={newHabitName}
                  onChange={(e) => setNewHabitName(e.target.value)}
                  className="w-full bg-zinc-800 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:ring-1 focus:ring-coral"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-400 block mb-2">Routine</label>
                <div className="grid grid-cols-2 gap-2" role="radiogroup" aria-label="Routine time">
                  {(['MORNING', 'AFTERNOON', 'EVENING', 'ANYTIME'] as const).map((r) => (
                    <button
                      key={r}
                      type="button"
                      role="radio"
                      aria-checked={newRoutine === r}
                      onClick={() => setNewRoutine(r)}
                      className={`py-2 rounded-xl text-xs font-semibold border transition focus:outline-none focus:ring-1 focus:ring-coral ${
                        newRoutine === r
                          ? 'bg-coral text-white border-coral'
                          : 'bg-zinc-800 border-zinc-700 text-zinc-300 hover:bg-zinc-750'
                      }`}
                    >
                      {r.charAt(0) + r.slice(1).toLowerCase()}
                    </button>
                  ))}
                </div>
              </div>

              <div className="pt-2 flex gap-2">
                <button
                  type="button"
                  onClick={() => setIsAddOpen(false)}
                  className="flex-1 py-2.5 rounded-xl text-xs font-semibold bg-zinc-800 text-zinc-300 hover:bg-zinc-700 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!newHabitName.trim()}
                  className="flex-1 py-2.5 rounded-xl text-xs font-bold bg-coral text-white hover:bg-coral-light active:scale-95 disabled:opacity-40 transition"
                >
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
