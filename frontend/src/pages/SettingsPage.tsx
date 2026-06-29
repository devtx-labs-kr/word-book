import { useEffect, useState } from 'react';
import { useSettings, useTheme } from '../context/SettingsContext';
import Toggle from '../components/Toggle';

const APP_VERSION = '1.0.0';
const ALGORITHM = 'SM-2 (Spaced Repetition)';

const NEW_CARDS_MAX = 1000;
const MAX_REVIEWS_MAX = 10000;

/** Validates a non-negative integer within [0, max]; returns an error message or null. */
function validateLimit(value: string, max: number, label: string): string | null {
  if (!/^\d+$/.test(value.trim())) {
    return `${label} must be a whole number between 0 and ${max}`;
  }
  const n = Number(value);
  if (n < 0 || n > max) {
    return `${label} must be between 0 and ${max}`;
  }
  return null;
}

/**
 * Settings screen (/settings, FR-9). Dark mode toggles persist immediately via the theme selector
 * (BR-S9); the learning limits and showPronunciation are local form state persisted together on
 * "Save Settings" (BR-S7/S8). Numeric limits are validated client-side (BR-S1/S2) with inline
 * errors that disable Save; the backend re-validates as a second line of defense.
 */
export default function SettingsPage() {
  const { settings, loading, updateSettings } = useSettings();
  const { darkMode, toggleTheme } = useTheme();

  const [newCardsPerDay, setNewCardsPerDay] = useState('20');
  const [maxReviewsPerDay, setMaxReviewsPerDay] = useState('200');
  const [showPronunciation, setShowPronunciation] = useState(true);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  // Initialize the form from the loaded settings.
  useEffect(() => {
    if (settings) {
      setNewCardsPerDay(String(settings.newCardsPerDay));
      setMaxReviewsPerDay(String(settings.maxReviewsPerDay));
      setShowPronunciation(settings.showPronunciation);
    }
  }, [settings]);

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 3000);
    return () => clearTimeout(timer);
  }, [toast]);

  const newCardsError = validateLimit(newCardsPerDay, NEW_CARDS_MAX, 'New cards / day');
  const maxReviewsError = validateLimit(maxReviewsPerDay, MAX_REVIEWS_MAX, 'Max reviews / day');
  const hasError = newCardsError !== null || maxReviewsError !== null;
  // Boot fetch failed → settings stays null while the form still renders defaults. Saving then is a
  // no-op (updateSettings guards on settings===null), so disable Save to avoid a false success toast
  // — the boot failure itself surfaces via the provider's global error toast (Finding #1).
  const canSave = settings !== null;

  const handleSave = async () => {
    if (hasError || !canSave) return;
    setSaving(true);
    try {
      await updateSettings({
        newCardsPerDay: Number(newCardsPerDay),
        maxReviewsPerDay: Number(maxReviewsPerDay),
        showPronunciation,
      });
      setToast('Settings saved.');
    } catch {
      // The failure is surfaced exactly once by the provider's global error toast (BR-S12); do not
      // raise a second local toast here for the same failure.
    } finally {
      setSaving(false);
    }
  };

  if (loading && !settings) {
    return (
      <main className="settings-page" aria-label="Settings">
        <h1>Settings</h1>
        <p data-testid="settings-loading">Loading…</p>
      </main>
    );
  }

  return (
    <main className="settings-page" aria-label="Settings">
      <h1>Settings</h1>

      <section aria-labelledby="study-settings-heading" className="settings-section">
        <h2 id="study-settings-heading">Study Settings</h2>

        <div className="settings-card">
          <label htmlFor="new-cards-per-day" className="settings-card-label">
            New cards / day
          </label>
          <input
            id="new-cards-per-day"
            type="number"
            inputMode="numeric"
            min={0}
            max={NEW_CARDS_MAX}
            className="settings-value-input"
            value={newCardsPerDay}
            onChange={(e) => setNewCardsPerDay(e.target.value)}
            aria-invalid={newCardsError !== null}
            aria-describedby={newCardsError ? 'new-cards-error' : undefined}
            data-testid="new-cards-input"
          />
        </div>
        {newCardsError && (
          <p
            id="new-cards-error"
            className="field-error"
            role="alert"
            data-testid="new-cards-error"
          >
            {newCardsError}
          </p>
        )}

        <div className="settings-card">
          <label htmlFor="max-reviews-per-day" className="settings-card-label">
            Max reviews / day
          </label>
          <input
            id="max-reviews-per-day"
            type="number"
            inputMode="numeric"
            min={0}
            max={MAX_REVIEWS_MAX}
            className="settings-value-input"
            value={maxReviewsPerDay}
            onChange={(e) => setMaxReviewsPerDay(e.target.value)}
            aria-invalid={maxReviewsError !== null}
            aria-describedby={maxReviewsError ? 'max-reviews-error' : undefined}
            data-testid="max-reviews-input"
          />
        </div>
        {maxReviewsError && (
          <p
            id="max-reviews-error"
            className="field-error"
            role="alert"
            data-testid="max-reviews-error"
          >
            {maxReviewsError}
          </p>
        )}

        <div className="settings-card">
          <Toggle
            label="Show pronunciation"
            description="Display the pronunciation on the flashcard."
            checked={showPronunciation}
            onChange={setShowPronunciation}
            data-testid="show-pronunciation-toggle"
          />
        </div>
      </section>

      <section aria-labelledby="appearance-heading" className="settings-section">
        <h2 id="appearance-heading">Appearance</h2>
        <div className="settings-card">
          <Toggle
            label="Dark mode"
            description="Switch between light and dark themes."
            checked={darkMode}
            onChange={toggleTheme}
            data-testid="dark-mode-toggle"
          />
        </div>
      </section>

      <button
        type="button"
        className="btn-primary settings-save"
        onClick={() => void handleSave()}
        disabled={hasError || saving || !canSave}
        data-testid="save-settings"
      >
        {saving ? 'Saving…' : 'Save Settings'}
      </button>

      <section aria-labelledby="about-heading" className="settings-section">
        <h2 id="about-heading">About</h2>
        <div className="settings-card">
          <span className="settings-card-label">Version</span>
          <span className="settings-about-value" data-testid="about-version">
            {APP_VERSION}
          </span>
        </div>
        <div className="settings-card">
          <span className="settings-card-label">Algorithm</span>
          <span className="settings-about-value" data-testid="about-algorithm">
            {ALGORITHM}
          </span>
        </div>
        <p className="settings-about-caption muted">About · WordBook Web {APP_VERSION}</p>
      </section>

      {toast && (
        <p className="toast" role="status" data-testid="settings-toast">
          {toast}
        </p>
      )}
    </main>
  );
}
