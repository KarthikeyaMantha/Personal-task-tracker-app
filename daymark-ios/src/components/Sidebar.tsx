import React from 'react';
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
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      {/* Drawer */}
      <div className="relative w-72 max-w-[80vw] bg-[#18181B] text-white h-full flex flex-col shadow-2xl z-10 pt-[env(safe-area-inset-top)] pb-[env(safe-area-inset-bottom)]">
        {/* Brand Header */}
        <div className="p-6 border-b border-[#27272A] flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-coral flex items-center justify-center shadow-lg shadow-coral/30">
              <Check className="w-6 h-6 text-white stroke-[3]" />
            </div>
            <div>
              <h2 className="text-lg font-bold tracking-tight text-white leading-tight">Daymark</h2>
              <p className="text-xs text-zinc-400">Mindful momentum</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-zinc-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Links */}
        <div className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          <button
            onClick={() => {
              onClose();
              onNavigateToSettings();
            }}
            className="w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-medium text-zinc-200 hover:bg-zinc-800/80 active:bg-zinc-800 transition"
          >
            <Settings className="w-5 h-5 text-coral" />
            <span>Settings</span>
          </button>

          <button
            onClick={() => {
              onClose();
              onNavigateToInsights();
            }}
            className="w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-medium text-zinc-200 hover:bg-zinc-800/80 active:bg-zinc-800 transition"
          >
            <TrendingUp className="w-5 h-5 text-coral" />
            <span>Insights & Analytics</span>
          </button>

          <button
            onClick={() => {
              onClose();
              onNavigateToWeeklyReview();
            }}
            className="w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-medium text-zinc-200 hover:bg-zinc-800/80 active:bg-zinc-800 transition"
          >
            <Calendar className="w-5 h-5 text-coral" />
            <span>Weekly Review</span>
          </button>

          <button
            onClick={() => {
              onClose();
              onNavigateToTemplates();
            }}
            className="w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-medium text-zinc-200 hover:bg-zinc-800/80 active:bg-zinc-800 transition"
          >
            <FileText className="w-5 h-5 text-coral" />
            <span>Day Templates</span>
          </button>
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-[#27272A] bg-[#141416]">
          <div className="flex items-center space-x-2 text-xs font-semibold text-zinc-300">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>100% Offline • Zero Tracking</span>
          </div>
          <p className="text-[11px] text-zinc-500 mt-1">Daymark iOS PWA v1.0</p>
        </div>
      </div>
    </div>
  );
};

