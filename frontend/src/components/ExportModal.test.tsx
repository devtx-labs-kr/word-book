import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ExportModal from './ExportModal';
import { apiClient } from '../api/apiClient';

vi.mock('../api/apiClient', () => ({
  apiClient: { download: vi.fn() },
}));

const downloadMock = apiClient.download as ReturnType<typeof vi.fn>;

const deck = {
  id: 'deck-1',
  name: 'Spanish',
  descriptionText: '',
  color: '#007AFF',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  totalCards: 5,
  dueCards: 1,
  newCards: 2,
};

describe('ExportModal', () => {
  afterEach(() => vi.clearAllMocks());

  it('shows the deck name, card count, and suggested filename', () => {
    render(<ExportModal deck={deck} onClose={vi.fn()} />);
    expect(screen.getByTestId('export-modal')).toBeInTheDocument();
    expect(screen.getByTestId('export-card-count')).toHaveTextContent('5 cards');
    expect(screen.getByTestId('export-filename').textContent).toMatch(
      /^Spanish_\d{4}-\d{2}-\d{2}\.json$/,
    );
  });

  it('triggers the download and reports success', async () => {
    downloadMock.mockResolvedValue(undefined);
    const onClose = vi.fn();
    const onExported = vi.fn();
    render(<ExportModal deck={deck} onClose={onClose} onExported={onExported} />);

    await userEvent.click(screen.getByTestId('export-download-btn'));

    await waitFor(() =>
      expect(downloadMock).toHaveBeenCalledWith('/api/decks/deck-1/export', expect.any(String)),
    );
    expect(onExported).toHaveBeenCalledWith(deck);
    expect(onClose).toHaveBeenCalled();
  });

  it('surfaces a download error and keeps the modal open', async () => {
    downloadMock.mockRejectedValue(new Error('Deck not found: deck-1'));
    const onClose = vi.fn();
    render(<ExportModal deck={deck} onClose={onClose} />);

    await userEvent.click(screen.getByTestId('export-download-btn'));

    await waitFor(() =>
      expect(screen.getByTestId('export-error')).toHaveTextContent('Deck not found: deck-1'),
    );
    expect(onClose).not.toHaveBeenCalled();
  });
});
