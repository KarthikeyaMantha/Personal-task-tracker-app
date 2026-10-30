import React, { useState, useEffect } from 'react';
import { X, Mic, Check, Calendar, Clock, Flag, Edit3 } from 'lucide-react';
import { useSpeechRecognition } from '../core/voice';
import type { Task } from '../types';

interface VoiceSheetProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirmTask: (taskData: Omit<Task, 'id' | 'createdAt'>) => Promise<void>;
}

export const VoiceSheet: React.FC<VoiceSheetProps> = ({
  isOpen,
  onClose,
  onConfirmTask,
}) => {
  const { isListening, transcript, parsedResult, startListening, stopListening, error } = useSpeechRecognition();
  const [editableTitle, setEditableTitle] = useState('');
  const [countdown, setCountdown] = useState<number | null>(null);

  useEffect(() => {
    if (isOpen) {
      startListening();
      setEditableTitle('');
      setCountdown(null);
    } else {
      stopListening();
    }
  }, [isOpen, startListening, stopListening]);

  useEffect(() => {
    if (parsedResult) {
      setEditableTitle(parsedResult.cleanTitle);
    }
  }, [parsedResult]);

  // Start 3-second countdown once speech finished and valid title parsed
  useEffect(() => {
    if (!isListening && parsedResult && parsedResult.cleanTitle.trim().length > 0) {
      setCountdown(3);
      const timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev === null || prev <= 1) {
            clearInterval(timer);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => clearInterval(timer);
    }
  }, [isListening, parsedResult]);

  // Handle countdown trigger
  useEffect(() => {
    if (countdown === 0 && parsedResult) {
      handleSaveNow();
    }
  }, [countdown]);

  if (!isOpen) return null;

  const handleSaveNow = async () => {
    if (!parsedResult) return;
    const finalTitle = editableTitle.trim() || parsedResult.cleanTitle;
    if (!finalTitle) return;

    await onConfirmTask({
      title: finalTitle,
      isCompleted: false,
      dueDate: parsedResult.dueDate,
      dueTime: parsedResult.dueTimeMinutes,
      priority: parsedResult.priority,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center">
      <div className="fixed inset-0 bg-black/75 backdrop-blur-sm" onClick={onClose} />

      <div className="relative w-full max-w-lg bg-[#18181B] text-white rounded-t-3xl p-6 shadow-2xl pb-[calc(env(safe-area-inset-bottom)+24px)] animate-in slide-in-from-bottom duration-200 text-center">
        {/* Header */}
        <div className="flex items-center justify-between pb-3">
          <h2 className="text-base font-bold text-white">Voice Action</h2>
          <button
            onClick={onClose}
            className="p-1 rounded-full text-zinc-400 hover:text-white hover:bg-zinc-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Pulsing Mic Visual */}
        <div className="my-6 flex flex-col items-center justify-center">
          <div className="relative flex items-center justify-center">
            {isListening && (
              <>
                <div className="absolute w-28 h-28 rounded-full bg-coral/20 animate-ping" />
                <div className="absolute w-24 h-24 rounded-full bg-coral/30" />
              </>
            )}
            <button
              onClick={isListening ? stopListening : startListening}
              className={`relative z-10 w-20 h-20 rounded-full flex items-center justify-center shadow-xl transition-all ${
                isListening
                  ? 'bg-coral shadow-coral/40 scale-105'
                  : 'bg-zinc-800 text-zinc-300 hover:bg-zinc-700'
              }`}
            >
              <Mic className="w-9 h-9 text-white stroke-[2.5]" />
            </button>
          </div>

          <p className="mt-4 text-sm font-semibold text-white">
            {isListening
              ? 'Listening...'
              : parsedResult
              ? 'Recognized!'
              : 'Tap microphone to speak'}
          </p>

          <p className="text-xs text-zinc-400 max-w-xs mt-1">
            {transcript ||
              'Try: "Remind me to call Mom tomorrow at 6pm" or "Review presentation !urgent"'}
          </p>

          {error && <p className="text-xs text-rose-400 mt-2 font-medium">{error}</p>}
        </div>

        {/* Task Preview Card */}
        {parsedResult && parsedResult.cleanTitle && (
          <div className="bg-[#202024] border border-zinc-700/80 rounded-2xl p-4 text-left shadow-lg space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold uppercase tracking-wider text-coral">
                Recognized Task
              </span>
              <div className="flex items-center space-x-1 text-xs text-zinc-400">
                <Edit3 className="w-3.5 h-3.5" />
                <span>Tap to edit</span>
              </div>
            </div>

            {/* Editable Title */}
            <input
              type="text"
              value={editableTitle}
              onChange={(e) => {
                setEditableTitle(e.target.value);
                setCountdown(null); // Cancel auto-countdown if user edits
              }}
              className="w-full bg-zinc-800/80 rounded-xl px-3 py-2 text-base font-semibold text-white focus:outline-none focus:ring-1 focus:ring-coral"
            />

            {/* Metadata Chips */}
            <div className="flex flex-wrap gap-2 pt-1">
              {parsedResult.dueDate && (
                <div className="flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-zinc-800 text-zinc-200">
                  <Calendar className="w-3.5 h-3.5 text-coral" />
                  <span>
                    {new Date(parsedResult.dueDate).toLocaleDateString(undefined, {
                      weekday: 'short',
                      month: 'short',
                      day: 'numeric',
                    })}
                  </span>
                </div>
              )}

              {parsedResult.dueTimeMinutes !== undefined && (
                <div className="flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-zinc-800 text-zinc-200">
                  <Clock className="w-3.5 h-3.5 text-coral" />
                  <span>
                    {Math.floor(parsedResult.dueTimeMinutes / 60) % 12 || 12}:
                    {(parsedResult.dueTimeMinutes % 60).toString().padStart(2, '0')}{' '}
                    {parsedResult.dueTimeMinutes >= 720 ? 'PM' : 'AM'}
                  </span>
                </div>
              )}

              {parsedResult.priority && (
                <div className="flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-rose-500/20 text-rose-300 border border-rose-500/30">
                  <Flag className="w-3.5 h-3.5 text-rose-400" />
                  <span>
                    {parsedResult.priority === 4
                      ? 'Urgent'
                      : parsedResult.priority === 3
                      ? 'High'
                      : parsedResult.priority === 2
                      ? 'Medium'
                      : 'Low'}
                  </span>
                </div>
              )}
            </div>

            {/* Save Button with countdown */}
            <div className="pt-2">
              <button
                onClick={handleSaveNow}
                className="w-full py-3 rounded-xl font-bold text-sm bg-coral text-white hover:bg-coral-light active:scale-[0.98] shadow-lg shadow-coral/30 flex items-center justify-center space-x-2 transition"
              >
                <Check className="w-4 h-4 stroke-[3]" />
                <span>
                  {countdown !== null && countdown > 0
                    ? `Save Task (auto-saving in ${countdown}s)`
                    : 'Save Task Now'}
                </span>
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

