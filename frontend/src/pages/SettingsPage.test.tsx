import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SettingsPage from './SettingsPage';
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

function renderPage() {
  return render(
    <SettingsProvider>
      <SettingsPage />
    </SettingsProvider>,
  );
}

describe('SettingsPage', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  beforeEach(() => {
    getMock.mockResolvedValue(defaults({ newCardsPerDay: 15, maxReviewsPerDay: 120 }));
  });

  it('renders the current settings values and the About section', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId('new-cards-input')).toHaveValue(15));
    expect(screen.getByTestId('max-reviews-input')).toHaveValue(120);
    expect(screen.getByTestId('about-version')).toHaveTextContent('1.0.0');
    expect(screen.getByTestId('about-algorithm')).toHaveTextContent('SM-2 (Spaced Repetition)');
  });

  it('shows an inline error and disables Save when a limit is out of range', async () => {
    renderPage();
    const input = await screen.findByTestId('new-cards-input');

    await userEvent.clear(input);
    await userEvent.type(input, '-5');

    expect(screen.getByTestId('new-cards-error')).toBeInTheDocument();
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByTestId('save-settings')).toBeDisabled();
  });

  it('rejects a value above the maximum', async () => {
    renderPage();
    const input = await screen.findByTestId('max-reviews-input');
    await userEvent.clear(input);
    await userEvent.type(input, '99999');
    expect(screen.getByTestId('max-reviews-error')).toBeInTheDocument();
    expect(screen.getByTestId('save-settings')).toBeDisabled();
  });

  it('saves valid form values via a full-body PUT and shows a success toast', async () => {
    putMock.mockResolvedValue(defaults({ newCardsPerDay: 10, maxReviewsPerDay: 120 }));
    renderPage();
    const input = await screen.findByTestId('new-cards-input');

    await userEvent.clear(input);
    await userEvent.type(input, '10');
    await userEvent.click(screen.getByTestId('save-settings'));

    await waitFor(() =>
      expect(putMock).toHaveBeenCalledWith(
        '/api/settings',
        expect.objectContaining({
          newCardsPerDay: 10,
          maxReviewsPerDay: 120,
          showPronunciation: true,
        }),
      ),
    );
    await waitFor(() => expect(screen.getByTestId('settings-toast')).toHaveTextContent('saved'));
  });

  it('surfaces a save failure via the global error toast (no silent failure, no double toast)', async () => {
    putMock.mockRejectedValue(new Error('save boom'));
    renderPage();
    const input = await screen.findByTestId('new-cards-input');

    await userEvent.clear(input);
    await userEvent.type(input, '10');
    await userEvent.click(screen.getByTestId('save-settings'));

    // The failure surfaces exactly once, via the provider's global error toast.
    await waitFor(() =>
      expect(screen.getByTestId('settings-error-toast')).toHaveTextContent('save boom'),
    );
    // No local success toast for a failed save.
    expect(screen.queryByTestId('settings-toast')).not.toBeInTheDocument();
  });

  it('disables Save and shows no success toast when settings failed to load', async () => {
    getMock.mockRejectedValue(new Error('load boom'));
    renderPage();

    // Boot fetch failed → form renders defaults but Save is disabled (settings === null),
    // so a no-op save can never produce a false "saved" toast (Finding #2).
    const save = await screen.findByTestId('save-settings');
    expect(save).toBeDisabled();
    expect(screen.queryByTestId('settings-toast')).not.toBeInTheDocument();
    expect(putMock).not.toHaveBeenCalled();
  });
});
