import React from 'react';
import { Plus, Mic, Timer, CheckCircle, AlertTriangle, Sparkles } from 'lucide-react';
import { TaskItem } from '../components/TaskItem';
import type { Task, Category, Habit } from '../types';

interface TodayScreenProps {
  tasks: Task[];
  habits: Habit[];
  categories: Category[];
  onToggleTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (id: number) => void;
  onSnoozeTask: (task: Task) => void;
  onOpenNewTask: () => void;
  onOpenVoice: () => void;
  onOpenFocus: () => void;
}

export const TodayScreen: React.FC<TodayScreenProps> = ({
  tasks,
  habits,
  categories,
  onToggleTask,
  onEditTask,
  onDeleteTask,
  onSnoozeTask,
  onOpenNewTask,
  onOpenVoice,
  onOpenFocus,
}) => {
  const now = new Date();
  const hour = now.getHours();
  const greeting =
    hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  const dateFormatted = now.toLocaleDateString(undefined, {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  });

  const todayStart = new Date().setHours(0, 0, 0, 0);
  const todayEnd = new Date().setHours(23, 59, 59, 999);

  const todayTasks = tasks.filter(
    (t) => t.dueDate && t.dueDate >= todayStart && t.dueDate <= todayEnd
  );

  const overdueTasks = tasks.filter(
    (t) => !t.isCompleted && t.dueDate && t.dueDate < todayStart
  );

  const completedTodayCount = todayTasks.filter((t) => t.isCompleted).length;
  const totalTodayCount = todayTasks.length;
  const taskProgress =
    totalTodayCount > 0 ? Math.round((completedTodayCount / totalTodayCount) * 100) : 0;

  return (
    <div className="space-y-6 pb-28">
      {/* Greeting Header */}
      <div>
        <h2 className="text-2xl font-black tracking-tight text-white">{greeting}</h2>
        <p className="text-sm text-zinc-400 font-medium mt-0.5">{dateFormatted}</p>
      </div>

      {/* Daily Rhythm Progress Card */}
      <section
        aria-label="Daily progress summary"
        className="bg-[#18181B] border border-[#27272A] rounded-3xl p-5 shadow-lg flex items-center justify-between"
      >
        <div className="flex items-center space-x-4">
          <div
            role="progressbar"
            aria-valuenow={taskProgress}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label={`${taskProgress}% tasks completed`}
            className="relative w-16 h-16 flex items-center justify-center"
          >
            {/* SVG Progress Ring */}
            <svg className="w-full h-full transform -rotate-90" aria-hidden="true">
              <circle
                cx="32"
                cy="32"
                r="26"
                stroke="currentColor"
                strokeWidth="5"
                className="text-zinc-800"
                fill="transparent"
              />
              <circle
                cx="32"
                cy="32"
                r="26"
                stroke="currentColor"
                strokeWidth="5"
                strokeDasharray={163.36}
                strokeDashoffset={163.36 - (163.36 * taskProgress) / 100}
                strokeLinecap="round"
                className="text-coral transition-all duration-500"
                fill="transparent"
              />
            </svg>
            <span className="absolute text-sm font-bold text-white">{taskProgress}%</span>
          </div>

          <div>
            <h3 className="text-base font-bold text-white leading-tight">Daily Rhythm</h3>
            <p className="text-xs text-zinc-400 mt-0.5">
              {totalTodayCount === 0
                ? 'All clear for today'
                : `${completedTodayCount} of ${totalTodayCount} completed`}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={onOpenFocus}
          aria-label="Start 25-minute focus session"
          className="px-3.5 py-2 rounded-xl text-xs font-bold bg-zinc-800 hover:bg-zinc-700 text-coral flex items-center space-x-1.5 transition active:scale-95 shadow-sm focus:outline-none focus:ring-1 focus:ring-coral"
        >
          <Timer className="w-3.5 h-3.5" aria-hidden="true" />
          <span>Focus (25m)</span>
        </button>
      </section>

      {/* Overdue Section */}
      {overdueTasks.length > 0 && (
        <section aria-label="Overdue attention" className="space-y-3">
          <div className="flex items-center space-x-2 text-rose-400">
            <AlertTriangle className="w-4 h-4" aria-hidden="true" />
            <h3 className="text-sm font-bold uppercase tracking-wider">
              Overdue Attention ({overdueTasks.length})
            </h3>
          </div>
          <div className="space-y-2">
            {overdueTasks.map((task) => (
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
        </section>
      )}

      {/* Today's Focus List */}
      <section aria-label="Today's focus" className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-white">Today's Focus</h3>
          <span className="text-xs text-zinc-400 font-semibold">
            {todayTasks.length} {todayTasks.length === 1 ? 'task' : 'tasks'}
          </span>
        </div>

        {todayTasks.length === 0 ? (
          <div className="bg-[#18181B] border border-[#27272A] rounded-3xl p-7 text-center space-y-4">
            <div className="w-12 h-12 mx-auto rounded-full bg-coral/10 text-coral flex items-center justify-center" aria-hidden="true">
              <Sparkles className="w-6 h-6 stroke-[2.2]" />
            </div>
            <div>
              <h4 className="text-base font-bold text-white">All Clear for Today</h4>
              <p className="text-xs text-zinc-400 mt-1 max-w-xs mx-auto">
                No tasks scheduled for today. Relax, or capture what's on your mind:
              </p>
            </div>
            <div className="flex justify-center gap-3 pt-1">
              <button
                type="button"
                onClick={onOpenVoice}
                aria-label="Create task with voice"
                className="px-4 py-2.5 rounded-xl text-xs font-bold bg-zinc-800 hover:bg-zinc-700 text-coral flex items-center space-x-2 transition active:scale-95 focus:outline-none focus:ring-1 focus:ring-coral"
              >
                <Mic className="w-4 h-4" aria-hidden="true" />
                <span>Voice Task</span>
              </button>
              <button
                type="button"
                onClick={onOpenNewTask}
                aria-label="Add task"
                className="px-4 py-2.5 rounded-xl text-xs font-bold bg-coral hover:bg-coral-light text-white shadow-md shadow-coral/30 flex items-center space-x-2 transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-coral"
              >
                <Plus className="w-4 h-4 stroke-[3]" aria-hidden="true" />
                <span>Add Task</span>
              </button>
            </div>
          </div>
        ) : (
          <div className="space-y-2">
            {todayTasks.map((task) => (
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
      </section>

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
