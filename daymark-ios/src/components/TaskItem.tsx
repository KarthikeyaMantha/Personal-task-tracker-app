import React from 'react';
import { Check, Clock, Calendar, Trash2, Moon, Flag } from 'lucide-react';
import type { Task, Category } from '../types';

interface TaskItemProps {
  task: Task;
  category?: Category;
  onToggleComplete: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDelete: (id: number) => void;
  onSnooze?: (task: Task) => void;
}

export const TaskItem: React.FC<TaskItemProps> = ({
  task,
  category,
  onToggleComplete,
  onEdit,
  onDelete,
  onSnooze,
}) => {
  const formatDue = () => {
    if (!task.dueDate) return null;
    const d = new Date(task.dueDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const diffDays = Math.round((d.getTime() - today.getTime()) / 86400000);

    let dateStr = '';
    if (diffDays === 0) dateStr = 'Today';
    else if (diffDays === 1) dateStr = 'Tomorrow';
    else if (diffDays === -1) dateStr = 'Yesterday';
    else {
      dateStr = d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
    }

    if (task.dueTime !== undefined) {
      const h = Math.floor(task.dueTime / 60) % 12 || 12;
      const m = (task.dueTime % 60).toString().padStart(2, '0');
      const ampm = task.dueTime >= 720 ? 'PM' : 'AM';
      dateStr += `, ${h}:${m} ${ampm}`;
    }
    return dateStr;
  };

  const dueText = formatDue();
  const isOverdue =
    !task.isCompleted &&
    task.dueDate &&
    task.dueDate < new Date().setHours(0, 0, 0, 0);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onEdit(task);
    }
  };

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={() => onEdit(task)}
      onKeyDown={handleKeyDown}
      aria-label={`Task: ${task.title}${dueText ? `, due ${dueText}` : ''}${task.isCompleted ? ', completed' : ''}`}
      className={`group relative flex items-center p-4 rounded-2xl bg-[#1A1A1E] border transition-all active:scale-[0.99] cursor-pointer shadow-sm focus:outline-none focus:ring-2 focus:ring-coral/50 ${
        task.isCompleted
          ? 'border-transparent opacity-60'
          : isOverdue
          ? 'border-rose-500/40 bg-rose-500/5'
          : 'border-[#27272A] hover:border-zinc-700'
      }`}
    >
      {/* Accessible Checkbox */}
      <button
        type="button"
        role="checkbox"
        aria-checked={task.isCompleted}
        aria-label={task.isCompleted ? `Mark "${task.title}" as incomplete` : `Mark "${task.title}" as complete`}
        onClick={(e) => {
          e.stopPropagation();
          onToggleComplete(task);
        }}
        className={`w-6 h-6 rounded-full flex items-center justify-center border transition-all mr-3 flex-shrink-0 focus:outline-none focus:ring-2 focus:ring-coral/70 ${
          task.isCompleted
            ? 'bg-coral border-coral shadow-sm shadow-coral/30'
            : 'border-zinc-500 hover:border-coral bg-transparent'
        }`}
      >
        {task.isCompleted && <Check className="w-3.5 h-3.5 text-white stroke-[3]" aria-hidden="true" />}
      </button>

      {/* Task Content */}
      <div className="flex-1 min-w-0 pr-2">
        <p
          className={`text-sm font-semibold truncate ${
            task.isCompleted
              ? 'line-through text-zinc-400'
              : 'text-white'
          }`}
        >
          {task.title}
        </p>

        {(dueText || category || task.priority) && (
          <div className="flex items-center flex-wrap gap-2 mt-1.5" aria-hidden="true">
            {category && (
              <span
                className="text-[10px] font-semibold px-2 py-0.5 rounded-md"
                style={{
                  backgroundColor: `${category.color}22`,
                  color: category.color,
                }}
              >
                {category.name}
              </span>
            )}

            {dueText && (
              <span
                className={`text-[11px] font-medium flex items-center space-x-1 ${
                  isOverdue ? 'text-rose-400 font-semibold' : 'text-zinc-400'
                }`}
              >
                <Clock className="w-3 h-3" />
                <span>{dueText}</span>
              </span>
            )}

            {task.priority && task.priority >= 3 && (
              <span
                className={`text-[10px] font-bold px-1.5 py-0.5 rounded flex items-center space-x-1 ${
                  task.priority === 4
                    ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                    : 'bg-coral/20 text-coral border border-coral/30'
                }`}
              >
                <Flag className="w-2.5 h-2.5" />
                <span>{task.priority === 4 ? 'Urgent' : 'High'}</span>
              </span>
            )}
          </div>
        )}
      </div>

      {/* Quick Action Buttons: Snooze & Delete */}
      <div className="flex items-center space-x-1 flex-shrink-0">
        {onSnooze && !task.isCompleted && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onSnooze(task);
            }}
            aria-label={`Snooze "${task.title}" to tomorrow`}
            className="p-2 rounded-xl text-zinc-400 hover:text-white hover:bg-zinc-800 transition focus:outline-none focus:ring-1 focus:ring-coral"
            title="Snooze to tomorrow"
          >
            <Moon className="w-4 h-4" aria-hidden="true" />
          </button>
        )}

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            if (task.id) onDelete(task.id);
          }}
          aria-label={`Delete "${task.title}"`}
          className="p-2 rounded-xl text-zinc-400 hover:text-rose-400 hover:bg-rose-500/10 transition focus:outline-none focus:ring-1 focus:ring-rose-500"
          title="Delete task"
        >
          <Trash2 className="w-4 h-4" aria-hidden="true" />
        </button>
      </div>
    </div>
  );
};
