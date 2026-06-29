import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ThemeToggle from './ThemeToggle';
import { SettingsProvider } from '../context/SettingsContext';
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

function renderToggle() {
  return render(
    <SettingsProvider>
      <ThemeToggle />
    </SettingsProvider>,
  );
}

describe('ThemeToggle', () => {
  beforeEach(() => {
    document.documentElement.removeAttribute('data-theme');
  });
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('is disabled while settings are still loading', () => {
    getMock.mockReturnValue(new Promise(() => {})); // never resolves
    renderToggle();
    expect(screen.getByTestId('theme-toggle')).toBeDisabled();
  });

  it('reflects state via aria-pressed and a state-dependent label, and toggles', async () => {
    getMock.mockResolvedValue(defaults({ darkMode: false }));
    putMock.mockResolvedValue(defaults({ darkMode: true }));
    renderToggle();

    const button = await screen.findByTestId('theme-toggle');
    await waitFor(() => expect(button).toBeEnabled());
    expect(button).toHaveAttribute('aria-pressed', 'false');
    expect(button).toHaveAttribute('aria-label', 'Switch to dark mode');

    await userEvent.click(button);

    await waitFor(() => expect(button).toHaveAttribute('aria-pressed', 'true'));
    expect(button).toHaveAttribute('aria-label', 'Switch to light mode');
    expect(putMock).toHaveBeenCalledWith(
      '/api/settings',
      expect.objectContaining({ darkMode: true }),
    );
  });
});
