import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import StatisticsPage from './StatisticsPage';
import { apiClient } from '../api/apiClient';
import { StatisticsResponse } from '../api/types';

vi.mock('../api/apiClient', () => ({
  apiClient: { get: vi.fn() },
}));

const getMock = apiClient.get as ReturnType<typeof vi.fn>;

function sampleStats(overrides: Partial<StatisticsResponse> = {}): StatisticsResponse {
  return {
    today: {
      cardsReviewed: 30,
      correctAnswers: 26,
      accuracy: 26 / 30,
      studyTimeSeconds: 420,
      sessionCount: 2,
    },
    overall: { totalCards: 50, newCards: 10, learningCards: 25, masteredCards: 15 },
    upcoming: { dueToday: 5, dueTomorrow: 3, dueThisWeek: 12 },
    recentSessions: [
      {
        sessionId: 's1',
        deckName: 'English',
        cardsReviewed: 20,
        accuracy: 0.9,
        startedAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
      },
    ],
    ...overrides,
  };
}

describe('StatisticsPage', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  beforeEach(() => {
    getMock.mockResolvedValue(sampleStats());
  });

  it('renders today, overall, upcoming, and recent sessions from the response', async () => {
    render(<StatisticsPage />);

    await waitFor(() => expect(screen.getByTestId('today-grid')).toBeInTheDocument());
    expect(screen.getByTestId('today-cards-reviewed')).toHaveTextContent('30');
    // accuracy 26/30 = 86.67 → 87%.
    expect(screen.getByTestId('today-accuracy')).toHaveTextContent('87%');
    // 420s → 7m.
    expect(screen.getByTestId('today-study-time')).toHaveTextContent('7m');
    expect(screen.getByTestId('today-sessions')).toHaveTextContent('2');

    expect(screen.getByTestId('overall-total')).toHaveTextContent('50');
    expect(screen.getByTestId('overall-new')).toHaveTextContent('10');
    expect(screen.getByTestId('overall-learning')).toHaveTextContent('25');
    expect(screen.getByTestId('overall-mastered')).toHaveTextContent('15');

    expect(screen.getByTestId('upcoming-line')).toHaveTextContent(
      'Today 5 · Tomorrow 3 · This week 12',
    );

    expect(screen.getByTestId('recent-list')).toBeInTheDocument();
    expect(screen.getByText('English')).toBeInTheDocument();
  });

  it('shows the empty state when there are no recent sessions', async () => {
    getMock.mockResolvedValue(sampleStats({ recentSessions: [] }));
    render(<StatisticsPage />);

    await waitFor(() =>
      expect(screen.getByTestId('recent-empty')).toHaveTextContent('No study sessions yet'),
    );
  });

  it('shows a loading indicator before the response resolves', () => {
    getMock.mockReturnValue(new Promise(() => {})); // never resolves
    render(<StatisticsPage />);
    expect(screen.getByTestId('stats-loading')).toBeInTheDocument();
  });

  it('surfaces a fetch failure via an error toast (no silent failure)', async () => {
    getMock.mockRejectedValue(new Error('stats boom'));
    render(<StatisticsPage />);

    await waitFor(() =>
      expect(screen.getByTestId('stats-error-toast')).toHaveTextContent('stats boom'),
    );
  });
});
