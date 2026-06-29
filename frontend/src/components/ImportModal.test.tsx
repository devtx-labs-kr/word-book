import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ImportModal from './ImportModal';
import { apiClient } from '../api/apiClient';

vi.mock('../api/apiClient', () => ({
  apiClient: { postRaw: vi.fn() },
}));

const postRawMock = apiClient.postRaw as ReturnType<typeof vi.fn>;

function jsonFile(text: string, name = 'deck.json') {
  return new File([text], name, { type: 'application/json' });
}

describe('ImportModal', () => {
  afterEach(() => vi.clearAllMocks());

  it('previews a valid file and shows the deck name and card count', async () => {
    postRawMock.mockResolvedValueOnce({
      valid: true,
      deckName: '기초 영어',
      cardCount: 2,
      duplicateName: null,
    });
    render(<ImportModal onClose={vi.fn()} onImported={vi.fn()} />);

    await userEvent.upload(screen.getByTestId('import-file-input'), jsonFile('{"version":"1.0"}'));

    await waitFor(() =>
      expect(screen.getByTestId('import-preview')).toHaveTextContent('기초 영어'),
    );
    expect(screen.getByTestId('import-preview')).toHaveTextContent('2 cards');
    expect(screen.getByTestId('import-confirm-btn')).toBeEnabled();
    expect(postRawMock).toHaveBeenCalledWith('/api/decks/import/preview', '{"version":"1.0"}');
  });

  it('shows the duplicate-name note when a same-named deck exists', async () => {
    postRawMock.mockResolvedValueOnce({
      valid: true,
      deckName: 'Spanish',
      cardCount: 1,
      duplicateName: 'Spanish (Imported)',
    });
    render(<ImportModal onClose={vi.fn()} onImported={vi.fn()} />);

    await userEvent.upload(screen.getByTestId('import-file-input'), jsonFile('{}'));

    await waitFor(() =>
      expect(screen.getByTestId('import-duplicate-note')).toHaveTextContent('Spanish (Imported)'),
    );
  });

  it('marks an invalid file and disables import', async () => {
    postRawMock.mockResolvedValueOnce({
      valid: false,
      deckName: null,
      cardCount: 0,
      duplicateName: null,
    });
    render(<ImportModal onClose={vi.fn()} onImported={vi.fn()} />);

    await userEvent.upload(screen.getByTestId('import-file-input'), jsonFile('garbage'));

    await waitFor(() => expect(screen.getByTestId('import-invalid')).toBeInTheDocument());
    expect(screen.getByTestId('import-confirm-btn')).toBeDisabled();
  });

  it('imports the deck and calls onImported on success', async () => {
    postRawMock
      .mockResolvedValueOnce({
        valid: true,
        deckName: 'Spanish',
        cardCount: 1,
        duplicateName: null,
      })
      .mockResolvedValueOnce({ id: 'deck-9', name: 'Spanish', totalCards: 1 });
    const onImported = vi.fn();
    const onClose = vi.fn();
    render(<ImportModal onClose={onClose} onImported={onImported} />);

    await userEvent.upload(screen.getByTestId('import-file-input'), jsonFile('{"v":1}'));
    await waitFor(() => expect(screen.getByTestId('import-confirm-btn')).toBeEnabled());
    await userEvent.click(screen.getByTestId('import-confirm-btn'));

    await waitFor(() =>
      expect(onImported).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'deck-9', name: 'Spanish' }),
      ),
    );
    expect(postRawMock).toHaveBeenCalledWith('/api/decks/import', '{"v":1}');
    expect(onClose).toHaveBeenCalled();
  });

  it('surfaces an import error as an alert', async () => {
    postRawMock
      .mockResolvedValueOnce({
        valid: true,
        deckName: 'Spanish',
        cardCount: 1,
        duplicateName: null,
      })
      .mockRejectedValueOnce(new Error('Invalid WordBook export format'));
    const onImported = vi.fn();
    render(<ImportModal onClose={vi.fn()} onImported={onImported} />);

    await userEvent.upload(screen.getByTestId('import-file-input'), jsonFile('{"v":1}'));
    await waitFor(() => expect(screen.getByTestId('import-confirm-btn')).toBeEnabled());
    await userEvent.click(screen.getByTestId('import-confirm-btn'));

    await waitFor(() =>
      expect(screen.getByTestId('import-error')).toHaveTextContent(
        'Invalid WordBook export format',
      ),
    );
    expect(onImported).not.toHaveBeenCalled();
  });
});
