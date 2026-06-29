import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import CardManagementPage from './CardManagementPage';
import { apiClient } from '../api/apiClient';

vi.mock('../api/apiClient', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

const getMock = apiClient.get as ReturnType<typeof vi.fn>;
const postMock = apiClient.post as ReturnType<typeof vi.fn>;
const putMock = apiClient.put as ReturnType<typeof vi.fn>;
const deleteMock = apiClient.delete as ReturnType<typeof vi.fn>;

const DECK = {
  id: 'deck-1',
  name: 'Spanish',
  descriptionText: '',
  color: '#007AFF',
  createdAt: '',
  updatedAt: '',
  totalCards: 1,
};

function card(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 'card-1',
    front: 'apple',
    back: '사과',
    example: null,
    pronunciation: '/ap/',
    notes: null,
    learningState: 'new',
    easeFactor: 2.5,
    interval: 0,
    repetitions: 0,
    nextReviewDate: '2026-01-01T00:00:00Z',
    lastReviewedAt: null,
    totalReviews: 0,
    correctReviews: 0,
    ...overrides,
  };
}

let cardsResult: unknown[];

function wireGet() {
  getMock.mockImplementation((path: string) => {
    if (path.includes('/cards')) return Promise.resolve(cardsResult);
    return Promise.resolve(DECK);
  });
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/decks/deck-1']}>
      <Routes>
        <Route path="/decks/:id" element={<CardManagementPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('CardManagementPage', () => {
  beforeEach(() => {
    cardsResult = [card()];
    wireGet();
  });
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders the deck name header and its cards', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId('card-row')).toBeInTheDocument());
    expect(screen.getByRole('heading', { name: 'Spanish' })).toBeInTheDocument();
    expect(screen.getByText('apple')).toBeInTheDocument();
  });

  it('requests cards filtered by the selected tab', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId('card-row')).toBeInTheDocument());
    await userEvent.click(screen.getByTestId('filter-due'));
    await waitFor(() =>
      expect(getMock).toHaveBeenCalledWith(expect.stringContaining('filter=Due')),
    );
  });

  it('debounces the card search into the query', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId('card-row')).toBeInTheDocument());
    await userEvent.type(screen.getByTestId('card-search'), 'app');
    await waitFor(
      () => expect(getMock).toHaveBeenCalledWith(expect.stringContaining('search=app')),
      { timeout: 1000 },
    );
  });

  it('adds a card via POST', async () => {
    postMock.mockResolvedValue(card({ id: 'card-2' }));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('card-row')).toBeInTheDocument());

    await userEvent.click(screen.getByTestId('add-card-button'));
    await userEvent.type(screen.getByTestId('card-front-input'), 'banana');
    await userEvent.type(screen.getByTestId('card-back-input'), '바나나');
    await userEvent.click(screen.getByTestId('card-form-submit'));

    await waitFor(() =>
      expect(postMock).toHaveBeenCalledWith(
        '/api/decks/deck-1/cards',
        expect.objectContaining({ front: 'banana', back: '바나나' }),
      ),
    );
  });

  it('edits a card via PUT to /api/cards/:id', async () => {
    putMock.mockResolvedValue(card({ front: 'apricot' }));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('card-row')).toBeInTheDocument());

    await userEvent.click(screen.getByTestId('card-edit'));
    const input = screen.getByTestId('card-front-input');
    await userEvent.clear(input);
    await userEvent.type(input, 'apricot');
    await userEvent.click(screen.getByTestId('card-form-submit'));

    await waitFor(() =>
      expect(putMock).toHaveBeenCalledWith(
        '/api/cards/card-1',
        expect.objectContaining({ front: 'apricot' }),
      ),
    );
  });

  it('deletes a card after confirmation', async () => {
    deleteMock.mockResolvedValue(undefined);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('card-row')).toBeInTheDocument());

    await userEvent.click(screen.getByTestId('card-delete'));
    const dialog = screen.getByTestId('confirm-dialog');
    await userEvent.click(within(dialog).getByTestId('confirm-accept'));

    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('/api/cards/card-1'));
  });

  it('shows an empty state when the deck has no cards', async () => {
    cardsResult = [];
    renderPage();
    await waitFor(() => expect(screen.getByTestId('empty-state')).toBeInTheDocument());
  });
});
