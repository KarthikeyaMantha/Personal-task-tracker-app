import React from 'react';
import { Calendar as CalendarIcon, CheckCircle2, ListTodo, Repeat, Menu, Mic, Timer, TrendingUp, Moon } from 'lucide-react';

interface BottomNavProps {
  currentTab: 'today' | 'tasks' | 'calendar' | 'habits';
  onTabChange: (tab: 'today' | 'tasks' | 'calendar' | 'habits') => void;
}

export const BottomNavigation: React.FC<BottomNavProps> = ({ currentTab, onTabChange }) => {
  const tabs = [
    { id: 'today', label: 'Today', icon: CalendarIcon },
    { id: 'tasks', label: 'Tasks', icon: ListTodo },
    { id: 'calendar', label: 'Calendar', icon: CheckCircle2 },
    { id: 'habits', label: 'Habits', icon: Repeat },
  ] as const;

  return (
    <nav
      aria-label="Bottom Navigation"
      className="fixed bottom-0 left-0 right-0 bg-[#18181B]/95 backdrop-blur-lg border-t border-[#27272A] z-40 bottom-nav-safe"
    >
      <div className="flex items-center justify-around h-16 max-w-lg mx-auto px-2">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isSelected = currentTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              aria-label={tab.label}
              aria-current={isSelected ? 'page' : undefined}
              className="flex flex-col items-center justify-center flex-1 h-full py-1 transition-all duration-200 focus:outline-none"
            >
              <div
                className={`flex items-center justify-center px-5 py-1.5 rounded-full transition-all duration-200 ${
                  isSelected ? 'bg-coral text-white shadow-md shadow-coral/30' : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                <Icon className="w-5 h-5 stroke-[2.2]" aria-hidden="true" />
              </div>
              <span
                className={`text-[11px] font-medium mt-1 transition-colors ${
                  isSelected ? 'text-coral font-semibold' : 'text-zinc-400'
                }`}
              >
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};

interface TopBarProps {
  title: string;
  onOpenSidebar: () => void;
  onOpenVoice: () => void;
  onOpenFocus: () => void;
  onOpenInsights: () => void;
  onOpenWrapUp: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  title,
  onOpenSidebar,
  onOpenVoice,
  onOpenFocus,
  onOpenInsights,
  onOpenWrapUp,
}) => {
  return (
    <header className="sticky top-0 z-30 bg-[#121214]/95 backdrop-blur-md pt-safe px-4 pb-3 border-b border-[#202024]">
      <div className="flex items-center justify-between max-w-lg mx-auto">
        <div className="flex items-center space-x-3">
          <button
            type="button"
            onClick={onOpenSidebar}
            className="p-2 -ml-2 rounded-xl text-zinc-300 hover:bg-zinc-800 transition active:scale-95 focus:outline-none focus:ring-1 focus:ring-coral"
            aria-label="Open sidebar menu"
          >
            <Menu className="w-6 h-6 stroke-[2]" aria-hidden="true" />
          </button>
          <h1 className="text-xl font-bold tracking-tight text-white">{title}</h1>
        </div>

        <div className="flex items-center space-x-1">
          <button
            type="button"
            onClick={onOpenVoice}
            className="p-2 rounded-xl text-coral hover:bg-coral/10 transition active:scale-95 focus:outline-none focus:ring-1 focus:ring-coral"
            aria-label="Voice input action"
          >
            <Mic className="w-5 h-5 stroke-[2.5]" aria-hidden="true" />
          </button>
          <button
            type="button"
            onClick={onOpenFocus}
            className="p-2 rounded-xl text-zinc-300 hover:bg-zinc-800 transition active:scale-95 focus:outline-none focus:ring-1 focus:ring-coral"
            aria-label="Open focus timer"
          >
            <Timer className="w-5 h-5 stroke-[2]" aria-hidden="true" />
          </button>
          <button
            type="button"
            onClick={onOpenInsights}
            className="p-2 rounded-xl text-zinc-300 hover:bg-zinc-800 transition active:scale-95 focus:outline-none focus:ring-1 focus:ring-coral"
            aria-label="Open insights"
          >
            <TrendingUp className="w-5 h-5 stroke-[2]" aria-hidden="true" />
          </button>
          <button
            type="button"
            onClick={onOpenWrapUp}
            className="p-2 rounded-xl text-zinc-300 hover:bg-zinc-800 transition active:scale-95 focus:outline-none focus:ring-1 focus:ring-coral"
            aria-label="Open daily wrap up"
          >
            <Moon className="w-5 h-5 stroke-[2]" aria-hidden="true" />
          </button>
        </div>
      </div>
    </header>
  );
};
