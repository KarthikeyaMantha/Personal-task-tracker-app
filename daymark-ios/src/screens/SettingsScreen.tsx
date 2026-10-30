import React from 'react';
import { ArrowLeft, Moon, Sun, Monitor, Download, ShieldCheck, Database, Trash2 } from 'lucide-react';
import { db } from '../db/db';

interface SettingsScreenProps {
  onBack: () => void;
  theme: 'system' | 'light' | 'dark';
  onThemeChange: (theme: 'system' | 'light' | 'dark') => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  onBack,
  theme,
  onThemeChange,
}) => {
  const handleExportData = async () => {
    const tasks = await db.tasks.toArray();
    const habits = await db.habits.toArray();
    const logs = await db.habitLogs.toArray();
    const categories = await db.categories.toArray();

    const backupData = {
      version: 1,
      exportedAt: new Date().toISOString(),
      tasks,
      habits,
      habitLogs: logs,
      categories,
    };

    const blob = new Blob([JSON.stringify(backupData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `daymark_backup_${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleClearAllData = async () => {
    if (confirm('Are you sure you want to delete all Daymark data? This cannot be undone.')) {
      await db.tasks.clear();
      await db.habits.clear();
      await db.habitLogs.clear();
      window.location.reload();
    }
  };

  return (
    <div className="space-y-6 pb-28 max-w-sm mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <button
          onClick={onBack}
          className="p-2 -ml-2 rounded-xl text-zinc-400 hover:text-white hover:bg-zinc-800 transition"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-lg font-bold text-white">Settings</h2>
        <div className="w-9" />
      </div>

      {/* Theme Preference */}
      <div className="bg-[#18181B] border border-[#27272A] rounded-2xl p-4 space-y-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-400">Appearance</h3>
        <div className="grid grid-cols-3 gap-2">
          {[
            { id: 'system', label: 'System', icon: Monitor },
            { id: 'light', label: 'Light', icon: Sun },
            { id: 'dark', label: 'Dark', icon: Moon },
          ].map((t) => {
            const Icon = t.icon;
            const isSel = theme === t.id;
            return (
              <button
                key={t.id}
                onClick={() => onThemeChange(t.id as any)}
                className={`flex flex-col items-center justify-center py-2.5 rounded-xl border text-xs font-semibold transition ${
                  isSel
                    ? 'bg-coral text-white border-coral shadow-sm shadow-coral/30'
                    : 'bg-zinc-800/80 border-zinc-700 text-zinc-300 hover:bg-zinc-700'
                }`}
              >
                <Icon className="w-4 h-4 mb-1" />
                <span>{t.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Data & Privacy */}
      <div className="bg-[#18181B] border border-[#27272A] rounded-2xl p-4 space-y-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-400">Data & Privacy</h3>
        <div className="space-y-2">
          <button
            onClick={handleExportData}
            className="w-full flex items-center justify-between p-3 rounded-xl bg-zinc-800/70 hover:bg-zinc-800 text-sm font-semibold text-zinc-200 transition"
          >
            <div className="flex items-center space-x-2.5">
              <Download className="w-4 h-4 text-coral" />
              <span>Export All Data (JSON)</span>
            </div>
            <span className="text-xs text-zinc-500">Backup</span>
          </button>

          <button
            onClick={handleClearAllData}
            className="w-full flex items-center justify-between p-3 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-sm font-semibold text-rose-400 transition"
          >
            <div className="flex items-center space-x-2.5">
              <Trash2 className="w-4 h-4" />
              <span>Clear All Data</span>
            </div>
            <span className="text-xs text-rose-400/80">Reset</span>
          </button>
        </div>
      </div>

      {/* Offline & Architecture Notice */}
      <div className="bg-[#18181B] border border-[#27272A] rounded-2xl p-5 space-y-2">
        <div className="flex items-center space-x-2 text-emerald-400 font-bold text-sm">
          <ShieldCheck className="w-5 h-5" />
          <span>100% Private & Local</span>
        </div>
        <p className="text-xs text-zinc-400 leading-relaxed">
          Daymark iOS stores all your tasks, habits, and focus logs locally on your device in IndexedDB. Nothing is ever sent to any remote server or cloud.
        </p>
      </div>
    </div>
  );
};

