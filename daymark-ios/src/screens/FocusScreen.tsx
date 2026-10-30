import React, { useState, useEffect } from 'react';
import { ArrowLeft, Play, Pause, RotateCcw, Check } from 'lucide-react';
import confetti from 'canvas-confetti';

interface FocusScreenProps {
  onBack: () => void;
}

export const FocusScreen: React.FC<FocusScreenProps> = ({ onBack }) => {
  const [mode, setMode] = useState<'focus' | 'shortBreak' | 'longBreak'>('focus');
  const [timeLeft, setTimeLeft] = useState(25 * 60);
  const [isRunning, setIsRunning] = useState(false);

  const totalTime = mode === 'focus' ? 25 * 60 : mode === 'shortBreak' ? 5 * 60 : 15 * 60;

  useEffect(() => {
    let interval: any = null;
    if (isRunning && timeLeft > 0) {
      interval = setInterval(() => {
        setTimeLeft((prev) => prev - 1);
      }, 1000);
    } else if (timeLeft === 0 && isRunning) {
      setIsRunning(false);
      try {
        confetti({ particleCount: 100, spread: 70, origin: { y: 0.6 } });
      } catch (e) {}
    }
    return () => clearInterval(interval);
  }, [isRunning, timeLeft]);

  const switchMode = (newMode: 'focus' | 'shortBreak' | 'longBreak') => {
    setMode(newMode);
    setIsRunning(false);
    setTimeLeft(newMode === 'focus' ? 25 * 60 : newMode === 'shortBreak' ? 5 * 60 : 15 * 60);
  };

  const resetTimer = () => {
    setIsRunning(false);
    setTimeLeft(totalTime);
  };

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;
  const progressPercent = ((totalTime - timeLeft) / totalTime) * 100;

  return (
    <div className="space-y-6 pb-28 text-center max-w-sm mx-auto">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={onBack}
          aria-label="Back to main screen"
          className="p-2 -ml-2 rounded-xl text-zinc-400 hover:text-white hover:bg-zinc-800 transition focus:outline-none focus:ring-1 focus:ring-coral"
        >
          <ArrowLeft className="w-5 h-5" aria-hidden="true" />
        </button>
        <h2 className="text-lg font-bold text-white">Focus Timer</h2>
        <div className="w-9" aria-hidden="true" />
      </div>

      {/* Mode Selector Chips */}
      <div className="flex justify-center gap-2 p-1 bg-[#18181B] border border-[#27272A] rounded-2xl" role="radiogroup" aria-label="Timer mode">
        {[
          { id: 'focus', label: 'Focus (25m)' },
          { id: 'shortBreak', label: 'Break (5m)' },
          { id: 'longBreak', label: 'Long (15m)' },
        ].map((m) => (
          <button
            key={m.id}
            type="button"
            role="radio"
            aria-checked={mode === m.id}
            onClick={() => switchMode(m.id as any)}
            className={`flex-1 py-1.5 rounded-xl text-xs font-semibold transition focus:outline-none focus:ring-1 focus:ring-coral ${
              mode === m.id
                ? 'bg-coral text-white shadow-md shadow-coral/30'
                : 'text-zinc-400 hover:text-zinc-200'
            }`}
          >
            {m.label}
          </button>
        ))}
      </div>

      {/* Circular Timer Visual */}
      <div
        className="relative w-64 h-64 mx-auto flex items-center justify-center my-8"
        role="timer"
        aria-label={`${minutes} minutes ${seconds} seconds remaining`}
      >
        <svg className="w-full h-full transform -rotate-90" aria-hidden="true">
          <circle
            cx="128"
            cy="128"
            r="110"
            stroke="currentColor"
            strokeWidth="10"
            className="text-zinc-800"
            fill="transparent"
          />
          <circle
            cx="128"
            cy="128"
            r="110"
            stroke="currentColor"
            strokeWidth="10"
            strokeDasharray={691.15}
            strokeDashoffset={691.15 - (691.15 * progressPercent) / 100}
            strokeLinecap="round"
            className="text-coral transition-all duration-300"
            fill="transparent"
          />
        </svg>

        <div className="absolute flex flex-col items-center">
          <span className="text-5xl font-black tracking-tight text-white font-mono" aria-hidden="true">
            {minutes.toString().padStart(2, '0')}:{seconds.toString().padStart(2, '0')}
          </span>
          <span className="text-xs font-medium text-zinc-400 uppercase tracking-widest mt-2">
            {isRunning ? 'Flow in progress' : 'Ready to focus'}
          </span>
        </div>
      </div>

      {/* Controls */}
      <div className="flex items-center justify-center gap-4">
        <button
          type="button"
          onClick={resetTimer}
          aria-label="Reset timer"
          className="p-3.5 rounded-2xl bg-zinc-800 text-zinc-300 hover:text-white hover:bg-zinc-700 transition active:scale-95 focus:outline-none focus:ring-1 focus:ring-coral"
          title="Reset timer"
        >
          <RotateCcw className="w-5 h-5" aria-hidden="true" />
        </button>

        <button
          type="button"
          onClick={() => setIsRunning(!isRunning)}
          aria-label={isRunning ? 'Pause focus timer' : 'Start focus timer'}
          className="py-3.5 px-8 rounded-2xl bg-coral text-white font-bold text-base shadow-xl shadow-coral/30 hover:bg-coral-light active:scale-95 flex items-center space-x-2 transition focus:outline-none focus:ring-2 focus:ring-coral/70"
        >
          {isRunning ? (
            <>
              <Pause className="w-5 h-5 fill-white" aria-hidden="true" />
              <span>Pause</span>
            </>
          ) : (
            <>
              <Play className="w-5 h-5 fill-white" aria-hidden="true" />
              <span>Start</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};
