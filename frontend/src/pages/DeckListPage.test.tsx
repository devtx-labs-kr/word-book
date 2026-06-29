import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import DeckListPage from './DeckListPage';
import { apiClient } from '../api/apiClient';

vi.mock('../api/apiClient', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    postRaw: vi.fn(),
    download: vi.fn(),
  },
}));

const getMock = apiClient.get as ReturnType<typeof vi.fn>;
const postMock = apiClient.post as ReturnType<typeof vi.fn>;
const deleteMock = apiClient.delete as ReturnType<typeof vi.fn>;

function deck(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 'deck-1',
    name: 'Spanish',
    descriptionText: '',
    color: '#007AFF',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    totalCards: 3,
    dueCards: 1,
    newCards: 2,
    ...overrides,
  };
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/decks']}>
      <DeckListPage />
    </MemoryRouter>,
  );
}

describe('DeckListPage', () => {
  beforeEach(() => {
    getMock.mockResolvedValue([deck()]);
  });
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('loads and renders decks with their stat badges', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId('deck-card')).toBeInTheDocument());
    expect(screen.getByText('Spanish')).toBeInTheDocument();
    expect(screen.getByTestId('deck-total')).toHaveTextContent('3');
    expect(screen.getByTestId('deck-due')).toHaveTextContent('1');
    expect(screen.getByTestId('deck-new')).toHaveTextContent('2');
  });

  it('requests a sorted list when the sort changes', async () => {
    renderPage();
    await waitFor(() => expect(getMock).toHaveBeenCalled());
    await userEvent.selectOptions(screen.getByTestId('deck-sort'), 'due');
    await waitFor(() => expect(getMock).toHaveBeenCalledWith(expect.stringContaining('sort=due')));
  });

  it('debounces search input into the query', async () => {
    renderPage();
    await waitFor(() => expect(getMock).toHaveBeenCalled());
    await userEvent.type(screen.getByTestId('deck-search'), 'span');
    await waitFor(
      () => expect(getMock).toHaveBeenCalledWith(expect.stringContaining('search=span')),
      { timeout: 1000 },
    );
  });

  it('creates a deck and reloads', async () => {
    postMock.mockResolvedValue(deck({ id: 'deck-2', name: 'French' }));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('deck-card')).toBeInTheDocument());

    await userEvent.click(screen.getByTestId('new-deck-button'));
    await userEvent.type(screen.getByTestId('deck-name-input'), 'French');
    await userEvent.click(screen.getByTestId('deck-form-submit'));

    await waitFor(() =>
      expect(postMock).toHaveBeenCalledWith(
        '/api/decks',
        expect.objectContaining({ name: 'French' }),
      ),
    );
  });

  it('deletes a deck after confirmation', async () => {
    deleteMock.mockResolvedValue(undefined);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('deck-card')).toBeInTheDocument());

    await userEvent.click(screen.getByTestId('deck-menu-toggle'));
    await userEvent.click(screen.getByTestId('deck-delete'));

    const dialog = screen.getByTestId('confirm-dialog');
    await userEvent.click(within(dialog).getByTestId('confirm-accept'));

    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('/api/decks/deck-1'));
  });

  it('shows an empty state when there are no decks', async () => {
    getMock.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('empty-state')).toBeInTheDocument());
    expect(screen.getByTestId('empty-create-deck')).toBeInTheDocument();
  });

  it('opens the import modal from the page-level Import button', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId('deck-card')).toBeInTheDocument());
    await userEvent.click(screen.getByTestId('deck-import-btn'));
    expect(screen.getByTestId('import-modal')).toBeInTheDocument();
  });

  it('opens the export modal from the deck ⋯ menu Export action', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId('deck-card')).toBeInTheDocument());
    await userEvent.click(screen.getByTestId('deck-menu-toggle'));
    await userEvent.click(screen.getByTestId('deck-export-action'));
    expect(screen.getByTestId('export-modal')).toBeInTheDocument();
  });
});
