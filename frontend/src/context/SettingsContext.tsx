import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import { apiClient } from '../api/apiClient';
import { SettingsResponse, UpdateSettingsRequest } from '../api/types';

/**
 * Applies the theme to the document root. Both light and dark always set the attribute (no
 * unset path) so the CSS `:root` / `[data-theme="dark"]` selectors stay in sync (BR-S9).
 */
function applyTheme(dark: boolean): void {
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
}

/** Auto-dismiss window (ms) for the global error toast — mirrors the SettingsPage success toast. */
const ERROR_TOAST_TIMEOUT_MS = 5000;

interface SettingsContextValue {
  settings: SettingsResponse | null;
  loading: boolean;
  error: string | null;
  /**
   * Merges `patch` onto the current settings and PUTs the full body (BR-S4). No-op while settings
   * are still loading (`settings === null`) so a partial body can never overwrite the single row
   * (SD-U6-2). Optimistic: applies locally (and the theme when `darkMode` is in the patch) before
   * the round trip, rolling back to the prior state on failure (BR-S12). On failure the `error`
   * state is set, which the provider renders as a global `role="alert"` toast — so immediate-persist
   * (darkMode) failures surface app-wide, not just on SettingsPage.
   */
  updateSettings: (patch: Partial<UpdateSettingsRequest>) => Promise<void>;
}

const SettingsContext = createContext<SettingsContextValue | undefined>(undefined);

export function SettingsProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState<SettingsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Auto-dismiss the global error toast after a few seconds so a transient failure does not pin a
  // stale message; a successful updateSettings also clears it eagerly (setError(null) below).
  useEffect(() => {
    if (error === null) return;
    const timer = setTimeout(() => setError(null), ERROR_TOAST_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [error]);

  // Boot: fetch the settings once, store them, and apply the persisted theme (PD-U6-3, RD-U6-1).
  useEffect(() => {
    let cancelled = false;
    apiClient
      .get<SettingsResponse>('/api/settings')
      .then((data) => {
        if (cancelled) return;
        setSettings(data);
        applyTheme(data.darkMode);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        // Stay on the default (light) theme; surface the failure rather than swallowing it.
        setError(e instanceof Error ? e.message : 'Failed to load settings');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const updateSettings = useCallback(
    async (patch: Partial<UpdateSettingsRequest>) => {
      // Guard (data integrity, SD-U6-2): never PUT a partial body before settings have loaded.
      if (settings === null) return;

      const previous = settings;
      const next: SettingsResponse = { ...settings, ...patch };

      // Optimistic local apply (+ theme when darkMode changed).
      setSettings(next);
      if ('darkMode' in patch) applyTheme(next.darkMode);
      setError(null);

      try {
        const saved = await apiClient.put<SettingsResponse>('/api/settings', next);
        setSettings(saved);
        if ('darkMode' in patch) applyTheme(saved.darkMode);
      } catch (e: unknown) {
        // Roll back to the prior server state, including the theme (BR-S12).
        setSettings(previous);
        applyTheme(previous.darkMode);
        setError(e instanceof Error ? e.message : 'Failed to save settings');
        throw e;
      }
    },
    [settings],
  );

  return (
    <SettingsContext.Provider value={{ settings, loading, error, updateSettings }}>
      {children}
      {error !== null && (
        <p className="toast toast-error" role="alert" data-testid="settings-error-toast">
          {error}
        </p>
      )}
    </SettingsContext.Provider>
  );
}

/** Access the settings store. Throws if used outside a {@link SettingsProvider}. */
export function useSettings(): SettingsContextValue {
  const context = useContext(SettingsContext);
  if (context === undefined) {
    throw new Error('useSettings must be used within a SettingsProvider');
  }
  return context;
}

/**
 * Thin theme selector over {@link useSettings}. `darkMode` reflects the current setting (false
 * while loading); `toggleTheme` flips it and persists immediately (BR-S9). The header ThemeToggle
 * stays disabled until settings load, so `toggleTheme` is only reachable once `settings` is set.
 */
export function useTheme(): { darkMode: boolean; toggleTheme: () => void } {
  const { settings, updateSettings } = useSettings();
  const darkMode = settings?.darkMode ?? false;
  const toggleTheme = useCallback(() => {
    // updateSettings already surfaces the failure via the context `error` state and rolls the
    // theme back (BR-S12); swallow the rejection here so the fire-and-forget toggle doesn't leave
    // an unhandled promise rejection (not a silent failure — the error is surfaced in context).
    void updateSettings({ darkMode: !darkMode }).catch(() => {});
  }, [darkMode, updateSettings]);
  return { darkMode, toggleTheme };
}
