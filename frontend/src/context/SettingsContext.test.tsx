import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SettingsProvider, useSettings, useTheme } from './SettingsContext';
import { apiClient } from '../api/apiClient';
import { SettingsResponse } from '../api/types';

vi.mock('../api/apiClient', () => ({
  apiClient: { get: vi.fn(), put: vi.fn() },
}));

const getMock = apiClient.get as ReturnType<typeof vi.fn>;
const putMock = apiClient.put as ReturnType<typeof vi.fn>;

function defaults(overrides: Partial<SettingsResponse> = {}): SettingsResponse {
  return {
    newCardsPerDay: 20,
    maxReviewsPerDay: 200,
    showPronunciation: true,
    darkMode: false,
    autoPlayAudio: false,
    studyReminderEnabled: false,
    studyReminderTime: null,
    ...overrides,
  };
}

/** Test harness exposing the context so we can drive updateSettings/useTheme. */
function Harness() {
  const { settings, updateSettings, error } = useSettings();
  const { darkMode, toggleTheme } = useTheme();
  return (
    <div>
      <span data-testid="new-cards">{settings ? settings.newCardsPerDay : 'null'}</span>
      <span data-testid="dark">{String(darkMode)}</span>
      <span data-testid="error">{error ?? ''}</span>
      <button data-testid="toggle-theme" onClick={toggleTheme}>
        toggle
      </button>
      <button data-testid="bump" onClick={() => void updateSettings({ newCardsPerDay: 5 })}>
        bump
      </button>
    </div>
  );
}

function renderHarness() {
  return render(
    <SettingsProvider>
      <Harness />
    </SettingsProvider>,
  );
}

describe('SettingsContext', () => {
  beforeEach(() => {
    document.documentElement.removeAttribute('data-theme');
  });
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('fetches settings on boot and applies the persisted theme', async () => {
    getMock.mockResolvedValue(defaults({ darkMode: true }));
    renderHarness();

    await waitFor(() => expect(screen.getByTestId('new-cards')).toHaveTextContent('20'));
    expect(getMock).toHaveBeenCalledWith('/api/settings');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('always sets data-theme (light included) on boot', async () => {
    getMock.mockResolvedValue(defaults({ darkMode: false }));
    renderHarness();
    await waitFor(() => expect(screen.getByTestId('new-cards')).toHaveTextContent('20'));
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('toggleTheme optimistically applies dark and PUTs the full body', async () => {
    getMock.mockResolvedValue(defaults({ darkMode: false }));
    putMock.mockResolvedValue(defaults({ darkMode: true }));
    renderHarness();
    await waitFor(() => expect(screen.getByTestId('dark')).toHaveTextContent('false'));

    await userEvent.click(screen.getByTestId('toggle-theme'));

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    await waitFor(() =>
      expect(putMock).toHaveBeenCalledWith(
        '/api/settings',
        expect.objectContaining({ darkMode: true, newCardsPerDay: 20, maxReviewsPerDay: 200 }),
      ),
    );
  });

  it('rolls back state and theme when the PUT fails (no silent failure)', async () => {
    getMock.mockResolvedValue(defaults({ darkMode: false }));
    putMock.mockRejectedValue(new Error('save boom'));
    renderHarness();
    await waitFor(() => expect(screen.getByTestId('dark')).toHaveTextContent('false'));

    await userEvent.click(screen.getByTestId('toggle-theme'));

    // Optimistic apply then rollback to the prior (light) state + surfaced error.
    await waitFor(() => expect(screen.getByTestId('dark')).toHaveTextContent('false'));
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(screen.getByTestId('error')).toHaveTextContent('save boom');
  });

  it('renders the darkMode-toggle PUT failure as a visible global error toast (role=alert)', async () => {
    getMock.mockResolvedValue(defaults({ darkMode: false }));
    putMock.mockRejectedValue(new Error('save boom'));
    renderHarness();
    await waitFor(() => expect(screen.getByTestId('dark')).toHaveTextContent('false'));

    await userEvent.click(screen.getByTestId('toggle-theme'));

    // The provider surfaces the failure as a visible toast app-wide (not just the context state).
    const toast = await screen.findByTestId('settings-error-toast');
    expect(toast).toHaveTextContent('save boom');
    expect(toast).toHaveAttribute('role', 'alert');
  });

  it('updateSettings is a no-op while settings are still null (partial-body guard)', async () => {
    // Never resolves -> settings stay null (loading).
    getMock.mockReturnValue(new Promise(() => {}));
    renderHarness();
    expect(screen.getByTestId('new-cards')).toHaveTextContent('null');

    await userEvent.click(screen.getByTestId('bump'));
    expect(putMock).not.toHaveBeenCalled();
  });

  it('surfaces a boot fetch failure as an error and stays on the default theme', async () => {
    getMock.mockRejectedValue(new Error('load boom'));
    renderHarness();
    await waitFor(() => expect(screen.getByTestId('error')).toHaveTextContent('load boom'));
    expect(screen.getByTestId('new-cards')).toHaveTextContent('null');
  });

  it('renders a boot fetch failure as a visible global error toast (role=alert)', async () => {
    getMock.mockRejectedValue(new Error('load boom'));
    renderHarness();
    const toast = await screen.findByTestId('settings-error-toast');
    expect(toast).toHaveTextContent('load boom');
    expect(toast).toHaveAttribute('role', 'alert');
  });
});
