import React, { useEffect } from 'react';
import { X, Settings, TrendingUp, Calendar, FileText, Check, ShieldCheck } from 'lucide-react';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  onNavigateToSettings: () => void;
  onNavigateToInsights: () => void;
  onNavigateToWeeklyReview: () => void;
  onNavigateToTemplates: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  isOpen,
  onClose,
  onNavigateToSettings,
  onNavigateToInsights,
  onNavigateToWeeklyReview,
  onNavigateToTemplates,
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Main menu drawer"
      className="fixed inset-0 z-50 flex"
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Drawer */}
      <div className="relative w-72 max-w-[80vw] bg-[#18181B] text-white h-full flex flex-col shadow-2xl z-10 pt-safe pb-safe">
        {/* Brand Header */}
        <div className="p-6 border-b border-[#27272A] flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-coral flex items-center justify-center shadow-lg shadow-coral/30" aria-hidden="true">
              <Check className="w-6 h-6 text-white stroke-[3]" />
            </div>
            <div>
              <h2 className="text-lg font-bold tracking-tight text-white leading-tight">Daymark</h2>
              <p className="text-xs text-zinc-400">Mindful momentum</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close menu"
            className="p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-zinc-800 transition focus:outline-none focus:ring-1 focus:ring-coral"
          >
            <X className="w-5 h-5" aria-hidden="true" />
          </button>
        </div>

        {/* Links */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto" aria-label="Sidebar navigation">
          <button
            type="button"
            onClick={() => {
              onClose();
              onNavigateToSettings();
            }}
            className="w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-medium text-zinc-200 hover:bg-zinc-800/80 active:bg-zinc-800 transition focus:outline-none focus:ring-1 focus:ring-coral"
          >
            <Settings className="w-5 h-5 text-coral" aria-hidden="true" />
            <span>Settings</span>
          </button>

          <button
            type="button"
            onClick={() => {
              onClose();
              onNavigateToInsights();
            }}
            className="w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-medium text-zinc-200 hover:bg-zinc-800/80 active:bg-zinc-800 transition focus:outline-none focus:ring-1 focus:ring-coral"
          >
            <TrendingUp className="w-5 h-5 text-coral" aria-hidden="true" />
            <span>Insights & Analytics</span>
          </button>

          <button
            type="button"
            onClick={() => {
              onClose();
              onNavigateToWeeklyReview();
            }}
            className="w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-medium text-zinc-200 hover:bg-zinc-800/80 active:bg-zinc-800 transition focus:outline-none focus:ring-1 focus:ring-coral"
          >
            <Calendar className="w-5 h-5 text-coral" aria-hidden="true" />
            <span>Weekly Review</span>
          </button>

          <button
            type="button"
            onClick={() => {
              onClose();
              onNavigateToTemplates();
            }}
            className="w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-medium text-zinc-200 hover:bg-zinc-800/80 active:bg-zinc-800 transition focus:outline-none focus:ring-1 focus:ring-coral"
          >
            <FileText className="w-5 h-5 text-coral" aria-hidden="true" />
            <span>Day Templates</span>
          </button>
        </nav>

        {/* Footer */}
        <div className="p-6 border-t border-[#27272A] bg-[#141416]">
          <div className="flex items-center space-x-2 text-xs text-zinc-400">
            <ShieldCheck className="w-4 h-4 text-emerald-400" aria-hidden="true" />
            <span>100% Offline & Private</span>
          </div>
          <p className="text-[11px] text-zinc-400 mt-1">
            Data saved locally in IndexedDB
          </p>
        </div>
      </div>
    </div>
  );
};
