import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import StudyPage from './StudyPage';
import StudyCompletePage from './StudyCompletePage';
import { apiClient } from '../api/apiClient';
import { SettingsProvider } from '../context/SettingsContext';
import { CardResponse } from '../api/types';

vi.mock('../api/apiClient', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

const getMock = apiClient.get as ReturnType<typeof vi.fn>;
const postMock = apiClient.post as ReturnType<typeof vi.fn>;

function card(id: string, front: string): CardResponse {
  return {
    id,
    front,
    back: `${front}-definition`,
    example: `${front} in a sentence`,
    pronunciation: `/${front}/`,
    notes: null,
    learningState: 'new',
    easeFactor: 2.5,
    interval: 0,
    repetitions: 0,
    nextReviewDate: '2026-01-01T00:00:00Z',
    lastReviewedAt: null,
    totalReviews: 0,
    correctReviews: 0,
  };
}

function mockStart(cards: CardResponse[]) {
  postMock.mockImplementation((path: string) => {
    if (path.endsWith('/study')) {
      return Promise.resolve({ sessionId: 's1', cards, total: cards.length });
    }
    if (path.endsWith('/answer')) {
      return Promise.resolve(card('updated', 'updated'));
    }
    if (path.endsWith('/complete')) {
      return Promise.resolve({
        cardsReviewed: cards.length,
        correctAnswers: cards.length,
        accuracy: 1,
        totalTimeSpent: 12,
      });
    }
    return Promise.reject(new Error(`unexpected ${path}`));
  });
}

function renderStudy() {
  return render(
    <SettingsProvider>
      <MemoryRouter initialEntries={['/study/deck-1']}>
        <Routes>
          <Route path="/study/:deckId" element={<StudyPage />} />
          <Route path="/study/:deckId/complete" element={<StudyCompletePage />} />
          <Route path="/decks" element={<div data-testid="decks-page">Decks</div>} />
        </Routes>
      </MemoryRouter>
    </SettingsProvider>,
  );
}

describe('StudyPage', () => {
  beforeEach(() => {
    getMock.mockResolvedValue({ id: 'deck-1', name: 'Spanish' });
  });
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('reveals the answer and rates through to the completion summary', async () => {
    mockStart([card('c1', 'apple')]);
    renderStudy();

    await waitFor(() => expect(screen.getByTestId('flashcard-front')).toBeInTheDocument());
    expect(screen.getByText('apple')).toBeInTheDocument();
    // Rating disabled until revealed.
    expect(screen.getByTestId('rating-good')).toBeDisabled();

    await userEvent.click(screen.getByTestId('flashcard-reveal'));
    expect(screen.getByTestId('flashcard-back')).toBeInTheDocument();
    expect(screen.getByTestId('rating-good')).toBeEnabled();

    await userEvent.click(screen.getByTestId('rating-good'));

    // Answer submitted with the integer wire rating + a cardId + timeSpentMs.
    await waitFor(() =>
      expect(postMock).toHaveBeenCalledWith(
        '/api/study/s1/answer',
        expect.objectContaining({ cardId: 'c1', rating: 2, timeSpentMs: expect.any(Number) }),
      ),
    );
    // Last card -> complete -> navigate to the summary.
    await waitFor(() => expect(screen.getByTestId('study-complete')).toBeInTheDocument());
    expect(postMock).toHaveBeenCalledWith('/api/study/s1/complete');
    expect(screen.getByTestId('summary-reviewed')).toHaveTextContent('1');
  });

  it('advances through multiple cards before completing', async () => {
    mockStart([card('c1', 'apple'), card('c2', 'banana')]);
    renderStudy();

    await waitFor(() => expect(screen.getByText('apple')).toBeInTheDocument());
    await userEvent.click(screen.getByTestId('flashcard-reveal'));
    await userEvent.click(screen.getByTestId('rating-good'));

    // Second card now shown, front side, not yet completed.
    await waitFor(() => expect(screen.getByText('banana')).toBeInTheDocument());
    expect(screen.getByTestId('flashcard-front')).toBeInTheDocument();
    expect(screen.queryByTestId('study-complete')).not.toBeInTheDocument();

    await userEvent.click(screen.getByTestId('flashcard-reveal'));
    await userEvent.click(screen.getByTestId('rating-easy'));

    await waitFor(() => expect(screen.getByTestId('study-complete')).toBeInTheDocument());
    expect(postMock).toHaveBeenCalledWith('/api/study/s1/complete');
  });

  it('supports keyboard: Space reveals, 1–4 rate', async () => {
    mockStart([card('c1', 'apple')]);
    renderStudy();

    await waitFor(() => expect(screen.getByTestId('flashcard-front')).toBeInTheDocument());

    await userEvent.keyboard(' ');
    expect(screen.getByTestId('flashcard-back')).toBeInTheDocument();

    await userEvent.keyboard('1');
    await waitFor(() =>
      expect(postMock).toHaveBeenCalledWith(
        '/api/study/s1/answer',
        expect.objectContaining({ rating: 0 }),
      ),
    );
  });

  it('ignores number keys until the card is revealed', async () => {
    mockStart([card('c1', 'apple')]);
    renderStudy();
    await waitFor(() => expect(screen.getByTestId('flashcard-front')).toBeInTheDocument());

    await userEvent.keyboard('3');
    // No answer submitted while the front is showing.
    expect(postMock.mock.calls.filter((c) => String(c[0]).endsWith('/answer'))).toHaveLength(0);
  });

  it('shows an empty state when there are no cards to study', async () => {
    mockStart([]);
    renderStudy();

    await waitFor(() => expect(screen.getByTestId('study-empty')).toBeInTheDocument());
    await userEvent.click(screen.getByTestId('study-empty'));
    await waitFor(() => expect(screen.getByTestId('decks-page')).toBeInTheDocument());
    // No complete call for an empty session.
    expect(postMock.mock.calls.filter((c) => String(c[0]).endsWith('/complete'))).toHaveLength(0);
  });

  it('exits mid-session via the confirm dialog and completes', async () => {
    mockStart([card('c1', 'apple'), card('c2', 'banana')]);
    renderStudy();

    await waitFor(() => expect(screen.getByText('apple')).toBeInTheDocument());
    await userEvent.click(screen.getByTestId('exit-study'));

    const dialog = screen.getByTestId('confirm-dialog');
    await userEvent.click(within(dialog).getByTestId('confirm-accept'));

    await waitFor(() => expect(screen.getByTestId('study-complete')).toBeInTheDocument());
    expect(postMock).toHaveBeenCalledWith('/api/study/s1/complete');
  });

  it('surfaces a start error as an alert toast', async () => {
    postMock.mockRejectedValue(new Error('boom'));
    renderStudy();
    await waitFor(() => expect(screen.getByTestId('error-message')).toHaveTextContent('boom'));
  });
});
