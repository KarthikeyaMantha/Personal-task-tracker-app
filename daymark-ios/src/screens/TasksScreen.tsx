import React, { useState } from 'react';
import { Search, Plus, Filter, CheckCircle2, Calendar, Clock, AlertCircle, X } from 'lucide-react';
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
  const [filter, setFilter] = useState<string>('today');
  const [searchQuery, setSearchQuery] = useState('');

  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const todayEnd = todayStart + 86400000 - 1;

  // Counts for chips
  const todayCount = tasks.filter(
    (t) => !t.isCompleted && t.dueDate && t.dueDate >= todayStart && t.dueDate <= todayEnd
  ).length;
  const overdueCount = tasks.filter(
    (t) => !t.isCompleted && t.dueDate && t.dueDate < todayStart
  ).length;
  const upcomingCount = tasks.filter(
    (t) => !t.isCompleted && t.dueDate && t.dueDate > todayEnd
  ).length;
  const inboxCount = tasks.filter((t) => !t.isCompleted && !t.dueDate).length;

  const filteredTasks = tasks.filter((task) => {
    // 1. View Filter
    if (filter === 'today') {
      if (!task.dueDate) return false;
      if (task.dueDate < todayStart || task.dueDate > todayEnd) return false;
    } else if (filter === 'inbox') {
      if (task.dueDate || task.isCompleted) return false;
    } else if (filter === 'upcoming') {
      if (!task.dueDate || task.dueDate <= todayEnd || task.isCompleted) return false;
    } else if (filter === 'overdue') {
      if (!task.dueDate || task.dueDate >= todayStart || task.isCompleted) return false;
    } else if (filter === 'completed') {
      if (!task.isCompleted) return false;
    }

    // 2. Search Filter
    if (searchQuery.trim()) {
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
        <label htmlFor="tasks-search-input" className="sr-only">Search tasks</label>
        <Search className="absolute left-3.5 top-1/2 transform -translate-y-1/2 w-4 h-4 text-zinc-400" aria-hidden="true" />
        <input
          id="tasks-search-input"
          type="text"
          placeholder="Search tasks..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full bg-[#18181B] border border-[#27272A] rounded-2xl pl-10 pr-9 py-2.5 text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-coral transition"
        />
        {searchQuery && (
          <button
            type="button"
            onClick={() => setSearchQuery('')}
            aria-label="Clear search query"
            className="absolute right-2 top-1/2 transform -translate-y-1/2 p-1.5 rounded-lg text-zinc-400 hover:text-white transition focus:outline-none focus:ring-1 focus:ring-coral"
          >
            <X className="w-4 h-4" aria-hidden="true" />
          </button>
        )}
      </div>

      {/* Primary View Filters (Today is First & Primary) */}
      <div className="flex gap-2 overflow-x-auto no-scrollbar py-1" role="radiogroup" aria-label="Task filters">
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
              type="button"
              role="radio"
              aria-checked={isSelected}
              onClick={() => setFilter(chip.id)}
              className={`flex items-center space-x-1.5 px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all focus:outline-none focus:ring-1 focus:ring-coral ${
                isSelected
                  ? 'bg-coral text-white shadow-md shadow-coral/30'
                  : 'bg-[#18181B] text-zinc-300 border border-[#27272A] hover:bg-zinc-800'
              }`}
            >
              <span>{chip.label}</span>
              {chip.count !== undefined && chip.count > 0 && (
                <span
                  className={`text-[10px] px-1.5 py-0.2 rounded-full font-bold ${
                    isSelected
                      ? 'bg-white/20 text-white'
                      : 'bg-zinc-800 text-zinc-400'
                  }`}
                >
                  {chip.count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Task List */}
      {filteredTasks.length === 0 ? (
        <div className="bg-[#18181B] border border-[#27272A] rounded-3xl p-8 text-center mt-6">
          <p className="text-sm font-semibold text-zinc-300">No tasks found</p>
          <p className="text-xs text-zinc-400 mt-1">
            Tap the + button to add what's on your mind.
          </p>
        </div>
      ) : (
        <div className="space-y-2 mt-2" role="feed" aria-label="Tasks list">
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
        type="button"
        onClick={onOpenNewTask}
        aria-label="Create task"
        className="fixed right-5 fab-bottom-safe z-30 w-14 h-14 rounded-full bg-coral text-white shadow-xl shadow-coral/40 flex items-center justify-center hover:bg-coral-light active:scale-95 transition-all focus:outline-none focus:ring-2 focus:ring-coral"
      >
        <Plus className="w-7 h-7 stroke-[2.8]" aria-hidden="true" />
      </button>
    </div>
  );
};
