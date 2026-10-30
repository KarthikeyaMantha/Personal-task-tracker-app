import React, { useState } from 'react';
import { Plus, Search, X } from 'lucide-react';
import { TaskItem } from '../components/TaskItem';
import type { Task, Category } from '../types';

interface TasksScreenProps {
  tasks: Task[];
  categories: Category[];
  onToggleTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (id: number) => void;
  onSnoozeTask: (task: Task) => void;
  onOpenNewTask: () => void;
}

export const TasksScreen: React.FC<TasksScreenProps> = ({
  tasks,
  categories,
  onToggleTask,
  onEditTask,
  onDeleteTask,
  onSnoozeTask,
  onOpenNewTask,
}) => {
  const [filter, setFilter] = useState<'today' | 'all' | 'inbox' | 'upcoming' | 'overdue' | 'completed'>('today');
  const [selectedCatId, setSelectedCatId] = useState<number | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  const todayStart = new Date().setHours(0, 0, 0, 0);
  const todayEnd = new Date().setHours(23, 59, 59, 999);

  // Compute counts
  const todayCount = tasks.filter((t) => t.dueDate && t.dueDate >= todayStart && t.dueDate <= todayEnd).length;
  const inboxCount = tasks.filter((t) => !t.isCompleted && !t.dueDate).length;
  const upcomingCount = tasks.filter((t) => !t.isCompleted && t.dueDate && t.dueDate > todayEnd).length;
  const overdueCount = tasks.filter((t) => !t.isCompleted && t.dueDate && t.dueDate < todayStart).length;

  const filteredTasks = tasks.filter((task) => {
    // 1. Primary View Filter
    if (filter === 'today') {
      if (!task.dueDate || task.dueDate < todayStart || task.dueDate > todayEnd) return false;
    } else if (filter === 'inbox') {
      if (task.isCompleted || task.dueDate) return false;
    } else if (filter === 'upcoming') {
      if (task.isCompleted || !task.dueDate || task.dueDate <= todayEnd) return false;
    } else if (filter === 'overdue') {
      if (task.isCompleted || !task.dueDate || task.dueDate >= todayStart) return false;
    } else if (filter === 'completed') {
      if (!task.isCompleted) return false;
    }

    // 2. Category Filter
    if (selectedCatId !== null && task.categoryId !== selectedCatId) {
      return false;
    }

    // 3. Search Query
    if (searchQuery.trim().length > 0) {
      const q = searchQuery.toLowerCase();
      const matchTitle = task.title.toLowerCase().includes(q);
      const matchDesc = task.description?.toLowerCase().includes(q);
      if (!matchTitle && !matchDesc) return false;
    }

    return true;
  });

  return (
    <div className="space-y-4 pb-28">
      {/* Search Input */}
      <div className="relative">
        <Search className="absolute left-3.5 top-1/2 transform -translate-y-1/2 w-4 h-4 text-zinc-400" />
        <input
          type="text"
          placeholder="Search tasks..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full bg-[#18181B] border border-[#27272A] rounded-2xl pl-10 pr-9 py-2.5 text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-coral transition"
        />
        {searchQuery && (
          <button
            onClick={() => setSearchQuery('')}
            className="absolute right-3 top-1/2 transform -translate-y-1/2 text-zinc-400 hover:text-white"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Primary View Filters (Today is First & Primary) */}
      <div className="flex gap-2 overflow-x-auto no-scrollbar py-1">
        {[
          { id: 'today', label: 'Today', count: todayCount },
          { id: 'all', label: 'All', count: tasks.length },
          { id: 'inbox', label: 'Inbox', count: inboxCount },
          { id: 'upcoming', label: 'Upcoming', count: upcomingCount },
          { id: 'overdue', label: 'Overdue', count: overdueCount },
          { id: 'completed', label: 'Completed' },
        ].map((chip) => {
          const isSelected = filter === chip.id;
          return (
            <button
              key={chip.id}
              onClick={() => setFilter(chip.id as any)}
              className={`flex items-center space-x-1.5 px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all ${
                isSelected
                  ? 'bg-coral text-white shadow-md shadow-coral/30'
                  : 'bg-[#18181B] text-zinc-300 border border-[#27272A] hover:bg-zinc-800'
              }`}
            >
              <span>{chip.label}</span>
              {chip.count !== undefined && chip.count > 0 && (
                <span
                  className={`text-[10px] px-1.5 py-0.2 rounded-full ${
                    isSelected ? 'bg-white/20 text-white' : 'bg-zinc-700 text-zinc-300'
                  }`}
                >
                  {chip.count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Category Filter Chips */}
      {categories.length > 0 && (
        <div className="flex gap-2 overflow-x-auto no-scrollbar py-1">
          <button
            onClick={() => setSelectedCatId(null)}
            className={`px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition ${
              selectedCatId === null
                ? 'bg-zinc-200 text-zinc-900 font-bold'
                : 'bg-[#18181B] text-zinc-400 border border-[#27272A]'
            }`}
          >
            All Categories
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => setSelectedCatId(selectedCatId === cat.id ? null : cat.id!)}
              className={`flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition ${
                selectedCatId === cat.id
                  ? 'bg-coral text-white shadow-sm'
                  : 'bg-[#18181B] text-zinc-300 border border-[#27272A]'
              }`}
            >
              <span className="w-2 h-2 rounded-full" style={{ backgroundColor: cat.color }} />
              <span>{cat.name}</span>
            </button>
          ))}
        </div>
      )}

      {/* Tasks List */}
      {filteredTasks.length === 0 ? (
        <div className="bg-[#18181B] border border-[#27272A] rounded-3xl p-8 text-center mt-6">
          <p className="text-sm font-semibold text-zinc-300">No matching tasks</p>
          <p className="text-xs text-zinc-500 mt-1">
            Tap the + button to add what's on your mind.
          </p>
        </div>
      ) : (
        <div className="space-y-2 mt-2">
          {filteredTasks.map((task) => (
            <TaskItem
              key={task.id}
              task={task}
              category={categories.find((c) => c.id === task.categoryId)}
              onToggleComplete={onToggleTask}
              onEdit={onEditTask}
              onDelete={onDeleteTask}
              onSnooze={onSnoozeTask}
            />
          ))}
        </div>
      )}

      {/* Floating Action Button (+) */}
      <button
        onClick={onOpenNewTask}
        className="fixed right-5 fab-bottom-safe z-30 w-14 h-14 rounded-full bg-coral text-white shadow-xl shadow-coral/40 flex items-center justify-center hover:bg-coral-light active:scale-95 transition-all"
        aria-label="Create task"
      >
        <Plus className="w-7 h-7 stroke-[2.8]" />
      </button>
    </div>
  );
};

