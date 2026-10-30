import React, { useState, useEffect } from 'react';
import { X, Calendar, Clock, Repeat as RepeatIcon, ChevronDown, ChevronUp, Trash2, Tag, Flag } from 'lucide-react';
import type { Task, Category } from '../types';

interface TaskSheetProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (taskData: Omit<Task, 'id' | 'createdAt'> & { id?: number }) => Promise<void>;
  onDelete?: (id: number) => Promise<void>;
  taskToEdit?: Task | null;
  categories: Category[];
}

export const TaskSheet: React.FC<TaskSheetProps> = ({
  isOpen,
  onClose,
  onSave,
  onDelete,
  taskToEdit,
  categories,
}) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDateType, setDueDateType] = useState<'none' | 'today' | 'tomorrow' | 'custom'>('today');
  const [customDate, setCustomDate] = useState('');
  const [timeType, setTimeType] = useState<'none' | 'morning' | 'afternoon' | 'evening' | 'custom'>('none');
  const [customTime, setCustomTime] = useState('');
  const [repeatType, setRepeatType] = useState<'none' | 'daily' | 'weekdays' | 'weekly'>('none');
  const [priority, setPriority] = useState<number>(0);
  const [categoryId, setCategoryId] = useState<number | undefined>(undefined);
  const [showMore, setShowMore] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  useEffect(() => {
    if (taskToEdit) {
      setTitle(taskToEdit.title);
      setDescription(taskToEdit.description || '');
      setPriority(taskToEdit.priority || 0);
      setCategoryId(taskToEdit.categoryId);

      if (taskToEdit.dueDate) {
        const d = new Date(taskToEdit.dueDate);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const tom = new Date(today.getTime() + 86400000);
        d.setHours(0, 0, 0, 0);

        if (d.getTime() === today.getTime()) setDueDateType('today');
        else if (d.getTime() === tom.getTime()) setDueDateType('tomorrow');
        else {
          setDueDateType('custom');
          setCustomDate(new Date(taskToEdit.dueDate).toISOString().split('T')[0]);
        }
      } else {
        setDueDateType('none');
      }

      if (taskToEdit.dueTime !== undefined) {
        if (taskToEdit.dueTime === 540) setTimeType('morning');
        else if (taskToEdit.dueTime === 780) setTimeType('afternoon');
        else if (taskToEdit.dueTime === 1080) setTimeType('evening');
        else {
          setTimeType('custom');
          const h = Math.floor(taskToEdit.dueTime / 60).toString().padStart(2, '0');
          const m = (taskToEdit.dueTime % 60).toString().padStart(2, '0');
          setCustomTime(`${h}:${m}`);
        }
      } else {
        setTimeType('none');
      }

      setRepeatType((taskToEdit.recurrenceRule as any) || 'none');
      if (taskToEdit.description || taskToEdit.priority || taskToEdit.categoryId) {
        setShowMore(true);
      }
    } else {
      setTitle('');
      setDescription('');
      setDueDateType('today');
      setCustomDate('');
      setTimeType('none');
      setCustomTime('');
      setRepeatType('none');
      setPriority(0);
      setCategoryId(undefined);
      setShowMore(false);
    }
  }, [taskToEdit, isOpen]);

  if (!isOpen) return null;

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    let computedDueDate: number | undefined;
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (dueDateType === 'today') computedDueDate = today.getTime();
    else if (dueDateType === 'tomorrow') computedDueDate = today.getTime() + 86400000;
    else if (dueDateType === 'custom' && customDate) {
      computedDueDate = new Date(`${customDate}T00:00:00`).getTime();
    }

    let computedDueTime: number | undefined;
    if (timeType === 'morning') computedDueTime = 540; // 9:00 AM
    else if (timeType === 'afternoon') computedDueTime = 780; // 1:00 PM
    else if (timeType === 'evening') computedDueTime = 1080; // 6:00 PM
    else if (timeType === 'custom' && customTime) {
      const [h, m] = customTime.split(':').map(Number);
      computedDueTime = h * 60 + m;
    }

    await onSave({
      id: taskToEdit?.id,
      title: title.trim(),
      description: description.trim() || undefined,
      isCompleted: taskToEdit?.isCompleted ?? false,
      dueDate: computedDueDate,
      dueTime: computedDueTime,
      priority: priority || undefined,
      categoryId: categoryId || undefined,
      recurrenceRule: repeatType !== 'none' ? repeatType : undefined,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center">
      <div className="fixed inset-0 bg-black/70 backdrop-blur-sm" onClick={onClose} />

      <div className="relative w-full max-w-lg bg-[#18181B] text-white rounded-t-3xl p-6 shadow-2xl max-h-[90vh] overflow-y-auto pb-[calc(env(safe-area-inset-bottom)+20px)] animate-in slide-in-from-bottom duration-200">
        <div className="flex items-center justify-between pb-4 border-b border-[#27272A]">
          <h2 className="text-lg font-bold text-white">
            {taskToEdit ? 'Edit Task' : 'New Task'}
          </h2>
          <button
            onClick={onClose}
            className="p-1 rounded-full text-zinc-400 hover:text-white hover:bg-zinc-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSave} className="space-y-5 mt-4">
          {/* Title Input */}
          <div>
            <input
              type="text"
              autoFocus
              placeholder="What needs to be done?"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full text-lg bg-transparent text-white placeholder-zinc-500 font-medium border-none focus:outline-none focus:ring-0"
            />
          </div>

          {/* Due Date Chips */}
          <div>
            <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider block mb-2">
              Due Date
            </label>
            <div className="flex flex-wrap gap-2">
              {[
                { id: 'none', label: 'No Date' },
                { id: 'today', label: 'Today' },
                { id: 'tomorrow', label: 'Tomorrow' },
                { id: 'custom', label: 'Pick Date' },
              ].map((chip) => (
                <button
                  key={chip.id}
                  type="button"
                  onClick={() => setDueDateType(chip.id as any)}
                  className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition-all ${
                    dueDateType === chip.id
                      ? 'bg-coral text-white shadow-md shadow-coral/30'
                      : 'bg-zinc-800 text-zinc-300 hover:bg-zinc-700'
                  }`}
                >
                  {chip.label}
                </button>
              ))}
            </div>
            {dueDateType === 'custom' && (
              <input
                type="date"
                value={customDate}
                onChange={(e) => setCustomDate(e.target.value)}
                className="mt-2 w-full px-3 py-2 bg-zinc-800 rounded-xl text-sm text-white focus:outline-none focus:ring-1 focus:ring-coral"
              />
            )}
          </div>

          {/* Reminder Time Chips */}
          <div>
            <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider block mb-2">
              Reminder Time
            </label>
            <div className="flex flex-wrap gap-2">
              {[
                { id: 'none', label: 'No Time' },
                { id: 'morning', label: 'Morning (9:00 AM)' },
                { id: 'afternoon', label: 'Afternoon (1:00 PM)' },
                { id: 'evening', label: 'Evening (6:00 PM)' },
              ].map((chip) => (
                <button
                  key={chip.id}
                  type="button"
                  onClick={() => setTimeType(chip.id as any)}
                  className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition-all ${
                    timeType === chip.id
                      ? 'bg-coral text-white shadow-md shadow-coral/30'
                      : 'bg-zinc-800 text-zinc-300 hover:bg-zinc-700'
                  }`}
                >
                  {chip.label}
                </button>
              ))}
            </div>
          </div>

          {/* Repeat Chips */}
          <div>
            <label className="text-xs font-semibold text-zinc-400 uppercase tracking-wider block mb-2">
              Repeat
            </label>
            <div className="flex flex-wrap gap-2">
              {[
                { id: 'none', label: 'Does not repeat' },
                { id: 'daily', label: 'Daily' },
                { id: 'weekdays', label: 'Weekdays' },
                { id: 'weekly', label: 'Weekly' },
              ].map((chip) => (
                <button
                  key={chip.id}
                  type="button"
                  onClick={() => setRepeatType(chip.id as any)}
                  className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition-all ${
                    repeatType === chip.id
                      ? 'bg-coral text-white shadow-md shadow-coral/30'
                      : 'bg-zinc-800 text-zinc-300 hover:bg-zinc-700'
                  }`}
                >
                  {chip.label}
                </button>
              ))}
            </div>
          </div>

          {/* Expandable More Options */}
          <div className="border-t border-[#27272A] pt-3">
            <button
              type="button"
              onClick={() => setShowMore(!showMore)}
              className="flex items-center justify-between w-full text-xs font-medium text-zinc-400 hover:text-zinc-200 py-1"
            >
              <span>More options (notes, priority, category)</span>
              {showMore ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            </button>

            {showMore && (
              <div className="space-y-4 pt-3">
                {/* Notes Input */}
                <div>
                  <textarea
                    placeholder="Add notes..."
                    rows={2}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="w-full bg-zinc-800/80 rounded-xl px-3.5 py-2 text-sm text-white placeholder-zinc-500 focus:outline-none focus:ring-1 focus:ring-coral resize-none"
                  />
                </div>

                {/* Priority Selection */}
                <div>
                  <label className="text-xs font-semibold text-zinc-400 block mb-2">Priority</label>
                  <div className="flex gap-2">
                    {[
                      { val: 0, label: 'None' },
                      { val: 1, label: 'Low', color: 'text-zinc-400' },
                      { val: 2, label: 'Medium', color: 'text-amber-400' },
                      { val: 3, label: 'High', color: 'text-coral' },
                      { val: 4, label: 'Urgent', color: 'text-rose-500' },
                    ].map((p) => (
                      <button
                        key={p.val}
                        type="button"
                        onClick={() => setPriority(p.val)}
                        className={`flex-1 py-1.5 rounded-lg text-xs font-semibold border transition ${
                          priority === p.val
                            ? 'bg-coral text-white border-coral shadow-sm shadow-coral/30'
                            : 'bg-zinc-800 border-zinc-700 text-zinc-300 hover:bg-zinc-700'
                        }`}
                      >
                        {p.label}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Category Selection */}
                {categories.length > 0 && (
                  <div>
                    <label className="text-xs font-semibold text-zinc-400 block mb-2">Category</label>
                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => setCategoryId(undefined)}
                        className={`px-3 py-1 rounded-full text-xs font-semibold transition ${
                          categoryId === undefined
                            ? 'bg-zinc-200 text-zinc-900 font-bold'
                            : 'bg-zinc-800 text-zinc-300'
                        }`}
                      >
                        General
                      </button>
                      {categories.map((cat) => (
                        <button
                          key={cat.id}
                          type="button"
                          onClick={() => setCategoryId(cat.id)}
                          className={`flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-semibold transition ${
                            categoryId === cat.id
                              ? 'bg-coral text-white'
                              : 'bg-zinc-800 text-zinc-300'
                          }`}
                        >
                          <span className="w-2 h-2 rounded-full" style={{ backgroundColor: cat.color }} />
                          <span>{cat.name}</span>
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Actions */}
          <div className="pt-2 flex gap-3">
            {taskToEdit && onDelete && (
              <button
                type="button"
                onClick={() => setShowDeleteConfirm(true)}
                className="flex-1 py-3.5 px-4 rounded-xl font-bold text-sm text-rose-400 border border-rose-500/30 hover:bg-rose-500/10 flex items-center justify-center space-x-2 transition"
              >
                <Trash2 className="w-4 h-4" />
                <span>Delete</span>
              </button>
            )}

            <button
              type="submit"
              disabled={!title.trim()}
              className="flex-[2] py-3.5 px-6 rounded-xl font-bold text-sm bg-coral text-white hover:bg-coral-light active:scale-[0.98] shadow-lg shadow-coral/30 disabled:opacity-40 disabled:pointer-events-none transition"
            >
              Save
            </button>
          </div>
        </form>

        {/* Delete Confirmation Modal */}
        {showDeleteConfirm && (
          <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
            <div className="bg-[#202024] p-5 rounded-2xl max-w-sm w-full border border-zinc-700 shadow-2xl text-center space-y-4">
              <h3 className="text-base font-bold text-white">Delete Task?</h3>
              <p className="text-xs text-zinc-300">
                Are you sure you want to permanently delete "{title}"? This cannot be undone.
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(false)}
                  className="flex-1 py-2.5 rounded-xl text-xs font-semibold bg-zinc-800 text-zinc-300 hover:bg-zinc-700"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={async () => {
                    if (taskToEdit?.id && onDelete) {
                      await onDelete(taskToEdit.id);
                    }
                    setShowDeleteConfirm(false);
                    onClose();
                  }}
                  className="flex-1 py-2.5 rounded-xl text-xs font-semibold bg-rose-500 text-white hover:bg-rose-600 shadow-md shadow-rose-500/30"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

