import { useSettings, useTheme } from '../context/SettingsContext';

/**
 * Header ☾/☀ theme toggle (FR-9.1). Replaces the AppShell placeholder button. Reflects state via
 * `aria-pressed` and a state-dependent `aria-label`. Disabled until settings load (`settings ===
 * null`) so a partial-body PUT can never be triggered before the full settings are known (SD-U6-2).
 */
export default function ThemeToggle() {
  const { settings } = useSettings();
  const { darkMode, toggleTheme } = useTheme();
  const isLoading = settings === null;

  return (
    <button
      type="button"
      className="theme-toggle"
      aria-pressed={darkMode}
      aria-label={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}
      disabled={isLoading}
      onClick={toggleTheme}
      data-testid="theme-toggle"
    >
      ☾ / ☀
    </button>
  );
}
