import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react';
import { TaskItem } from '../components/TaskItem';
import type { Task, Category } from '../types';

interface CalendarScreenProps {
  tasks: Task[];
  categories: Category[];
  onToggleTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (id: number) => void;
  onOpenNewTaskForDate: (date: Date) => void;
}

export const CalendarScreen: React.FC<CalendarScreenProps> = ({
  tasks,
  categories,
  onToggleTask,
  onEditTask,
  onDeleteTask,
  onOpenNewTaskForDate,
}) => {
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(new Date());

  const year = currentMonth.getFullYear();
  const month = currentMonth.getMonth();

  const firstDayOfMonth = new Date(year, month, 1).getDay();
  // Adjust so Monday is 0, Sunday is 6
  const startingDay = (firstDayOfMonth + 6) % 7;
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const prevMonth = () => setCurrentMonth(new Date(year, month - 1, 1));
  const nextMonth = () => setCurrentMonth(new Date(year, month + 1, 1));

  const isToday = (d: number) => {
    const today = new Date();
    return (
      today.getFullYear() === year &&
      today.getMonth() === month &&
      today.getDate() === d
    );
  };

  const isSelected = (d: number) => {
    return (
      selectedDate.getFullYear() === year &&
      selectedDate.getMonth() === month &&
      selectedDate.getDate() === d
    );
  };

  // Get tasks for a specific day
  const getTasksForDay = (d: number) => {
    const dayStart = new Date(year, month, d, 0, 0, 0, 0).getTime();
    const dayEnd = new Date(year, month, d, 23, 59, 59, 999).getTime();
    return tasks.filter((t) => t.dueDate && t.dueDate >= dayStart && t.dueDate <= dayEnd);
  };

  const selectedDayTasks = getTasksForDay(selectedDate.getDate());

  return (
    <div className="space-y-4 pb-28">
      {/* Month Navigation Header */}
      <div className="flex items-center justify-between px-2">
        <h2 className="text-lg font-bold text-white">
          {currentMonth.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}
        </h2>
        <div className="flex space-x-1">
          <button
            onClick={prevMonth}
            className="p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-zinc-800"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <button
            onClick={nextMonth}
            className="p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-zinc-800"
          >
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Clear Legend Row */}
      <div className="flex items-center justify-around py-2 px-3 bg-[#18181B] border border-[#27272A] rounded-2xl text-[11px] font-semibold text-zinc-300">
        <div className="flex items-center space-x-1.5">
          <span className="w-2.5 h-2.5 rounded-full bg-coral shadow-sm shadow-coral/30" />
          <span>Selected</span>
        </div>
        <div className="flex items-center space-x-1.5">
          <span className="w-2.5 h-2.5 rounded-full border-2 border-coral" />
          <span>Today</span>
        </div>
        <div className="flex items-center space-x-1.5">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
          <span>Completed</span>
        </div>
        <div className="flex items-center space-x-1.5">
          <span className="w-2.5 h-2.5 rounded-full bg-zinc-500" />
          <span>Scheduled</span>
        </div>
      </div>

      {/* Weekday Column Headers */}
      <div className="grid grid-cols-7 gap-1 text-center text-xs font-bold text-zinc-500 py-1">
        {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((day, idx) => (
          <div key={idx}>{day}</div>
        ))}
      </div>

      {/* Calendar Grid */}
      <div className="grid grid-cols-7 gap-1">
        {Array.from({ length: startingDay }).map((_, idx) => (
          <div key={`empty-${idx}`} className="h-10" />
        ))}

        {Array.from({ length: daysInMonth }).map((_, idx) => {
          const dayNum = idx + 1;
          const dayTasks = getTasksForDay(dayNum);
          const hasCompleted = dayTasks.some((t) => t.isCompleted);
          const hasPending = dayTasks.some((t) => !t.isCompleted);
          const sel = isSelected(dayNum);
          const tod = isToday(dayNum);

          return (
            <button
              key={dayNum}
              onClick={() => setSelectedDate(new Date(year, month, dayNum))}
              className={`h-11 rounded-xl flex flex-col items-center justify-center transition-all ${
                sel
                  ? 'bg-coral text-white font-bold shadow-md shadow-coral/30'
                  : tod
                  ? 'border-2 border-coral text-coral font-bold bg-[#18181B]'
                  : 'hover:bg-zinc-800 text-zinc-200'
              }`}
            >
              <span className="text-xs leading-none">{dayNum}</span>
              <div className="flex space-x-0.5 mt-1">
                {sel ? (
                  dayTasks.length > 0 && <span className="w-1 h-1 rounded-full bg-white" />
                ) : (
                  <>
                    {hasCompleted && <span className="w-1 h-1 rounded-full bg-emerald-400" />}
                    {hasPending && <span className="w-1 h-1 rounded-full bg-zinc-400" />}
                  </>
                )}
              </div>
            </button>
          );
        })}
      </div>

      {/* Selected Date Tasks Card */}
      <div className="pt-4 space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-white">
            {selectedDate.toLocaleDateString(undefined, {
              weekday: 'long',
              month: 'long',
              day: 'numeric',
            })}
          </h3>
          <span className="text-xs text-zinc-400 font-semibold">
            {selectedDayTasks.length} {selectedDayTasks.length === 1 ? 'task' : 'tasks'}
          </span>
        </div>

        {selectedDayTasks.length === 0 ? (
          <div className="bg-[#18181B] border border-[#27272A] rounded-2xl p-6 text-center">
            <p className="text-xs text-zinc-400">No tasks scheduled for this day.</p>
            <button
              onClick={() => onOpenNewTaskForDate(selectedDate)}
              className="mt-3 px-3.5 py-1.5 rounded-xl text-xs font-semibold bg-coral text-white shadow-sm inline-flex items-center space-x-1"
            >
              <Plus className="w-3.5 h-3.5 stroke-[3]" />
              <span>Add task for this day</span>
            </button>
          </div>
        ) : (
          <div className="space-y-2">
            {selectedDayTasks.map((task) => (
              <TaskItem
                key={task.id}
                task={task}
                category={categories.find((c) => c.id === task.categoryId)}
                onToggleComplete={onToggleTask}
                onEdit={onEditTask}
                onDelete={onDeleteTask}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

