import React, { useState } from 'react';
import { Plus, Flame, Check, Droplets, Clock, Trash2 } from 'lucide-react';
import type { Habit } from '../types';

interface HabitsScreenProps {
  habits: Habit[];
  onToggleHabit: (habit: Habit) => void;
  onAddHabit: (habitData: Omit<Habit, 'id' | 'createdAt' | 'currentStreak' | 'longestStreak'>) => Promise<void>;
  onDeleteHabit: (id: number) => Promise<void>;
}

export const HabitsScreen: React.FC<HabitsScreenProps> = ({
  habits,
  onToggleHabit,
  onAddHabit,
  onDeleteHabit,
}) => {
  const [selectedRoutine, setSelectedRoutine] = useState<string | null>(null);
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [newHabitName, setNewHabitName] = useState('');
  const [newRoutine, setNewRoutine] = useState<'MORNING' | 'AFTERNOON' | 'EVENING' | 'ANYTIME'>('MORNING');

  const routines = [
    { id: null, label: 'All Routines' },
    { id: 'MORNING', label: '🌅 Morning' },
    { id: 'AFTERNOON', label: '☀️ Afternoon' },
    { id: 'EVENING', label: '🌙 Evening' },
    { id: 'ANYTIME', label: '⚡ Anytime' },
  ];

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
      <div className="flex gap-2 overflow-x-auto no-scrollbar py-1">
        {routines.map((r) => {
          const isSelected = selectedRoutine === r.id;
          return (
            <button
              key={r.label}
              onClick={() => setSelectedRoutine(r.id)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all ${
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
          <p className="text-xs text-zinc-500">
            Build mindful consistency by creating your daily rituals.
          </p>
          <button
            onClick={() => setIsAddOpen(true)}
            className="px-4 py-2 rounded-xl text-xs font-bold bg-coral text-white shadow-md shadow-coral/30 inline-flex items-center space-x-1.5"
          >
            <Plus className="w-4 h-4 stroke-[3]" />
            <span>Create First Habit</span>
          </button>
        </div>
      ) : (
        <div className="space-y-2.5">
          {filteredHabits.map((habit) => (
            <div
              key={habit.id}
              className="flex items-center justify-between p-4 rounded-2xl bg-[#1A1A1E] border border-[#27272A] shadow-sm"
            >
              <div className="flex items-center space-x-3.5">
                <button
                  onClick={() => onToggleHabit(habit)}
                  className="w-8 h-8 rounded-full border-2 border-zinc-600 hover:border-coral flex items-center justify-center transition active:scale-90"
                >
                  <Check className="w-4 h-4 text-zinc-500 hover:text-coral transition" />
                </button>
                <div>
                  <h4 className="text-sm font-bold text-white">{habit.name}</h4>
                  <div className="flex items-center space-x-2 mt-1">
                    <span className="flex items-center space-x-1 text-xs font-semibold text-amber-400">
                      <Flame className="w-3.5 h-3.5 fill-amber-400" />
                      <span>{habit.currentStreak} day streak</span>
                    </span>
                    <span className="text-zinc-600">•</span>
                    <span className="text-[11px] text-zinc-400 capitalize">
                      {habit.routineTimeOfDay?.toLowerCase() || 'daily'}
                    </span>
                  </div>
                </div>
              </div>

              <button
                onClick={() => habit.id && onDeleteHabit(habit.id)}
                className="p-2 rounded-lg text-zinc-500 hover:text-rose-400 hover:bg-rose-500/10 transition"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Floating Action Button (+) */}
      <button
        onClick={() => setIsAddOpen(true)}
        className="fixed right-5 fab-bottom-safe z-30 w-14 h-14 rounded-full bg-coral text-white shadow-xl shadow-coral/40 flex items-center justify-center hover:bg-coral-light active:scale-95 transition-all"
        aria-label="Add habit"
      >
        <Plus className="w-7 h-7 stroke-[2.8]" />
      </button>

      {/* Add Habit Modal */}
      {isAddOpen && (
        <div className="fixed inset-0 z-50 flex items-end justify-center p-4">
          <div className="fixed inset-0 bg-black/70 backdrop-blur-sm" onClick={() => setIsAddOpen(false)} />
          <div className="relative w-full max-w-sm bg-[#202024] border border-zinc-700 rounded-3xl p-6 shadow-2xl space-y-4">
            <h3 className="text-base font-bold text-white">New Habit</h3>
            <form onSubmit={handleCreate} className="space-y-4">
              <input
                type="text"
                autoFocus
                placeholder="e.g. Morning 10-minute stretch"
                value={newHabitName}
                onChange={(e) => setNewHabitName(e.target.value)}
                className="w-full bg-zinc-800 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:ring-1 focus:ring-coral"
              />

              <div>
                <label className="text-xs font-semibold text-zinc-400 block mb-2">Routine</label>
                <div className="grid grid-cols-2 gap-2">
                  {(['MORNING', 'AFTERNOON', 'EVENING', 'ANYTIME'] as const).map((r) => (
                    <button
                      key={r}
                      type="button"
                      onClick={() => setNewRoutine(r)}
                      className={`py-2 rounded-xl text-xs font-semibold border transition ${
                        newRoutine === r
                          ? 'bg-coral text-white border-coral'
                          : 'bg-zinc-800 border-zinc-700 text-zinc-300'
                      }`}
                    >
                      {r.charAt(0) + r.slice(1).toLowerCase()}
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsAddOpen(false)}
                  className="flex-1 py-2.5 rounded-xl text-xs font-semibold bg-zinc-800 text-zinc-300"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!newHabitName.trim()}
                  className="flex-1 py-2.5 rounded-xl text-xs font-semibold bg-coral text-white shadow-md shadow-coral/30 disabled:opacity-40"
                >
                  Create Habit
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

