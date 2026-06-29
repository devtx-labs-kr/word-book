import { useRef, useState } from 'react';
import { apiClient } from '../api/apiClient';
import { DeckSummaryResponse } from '../api/types';
import { useFocusTrap } from '../hooks/useFocusTrap';

interface ExportModalProps {
  deck: DeckSummaryResponse;
  onClose: () => void;
  /** Called after a successful download so the page can show a success toast. */
  onExported?: (deck: DeckSummaryResponse) => void;
}

/** Sanitizes a deck name into the suggested download filename (mirrors the backend rule). */
function suggestedFilename(name: string): string {
  const sanitized = name.replace(/\//g, '-').replace(/:/g, '-').trim() || 'deck';
  const date = new Date().toISOString().slice(0, 10); // yyyy-MM-dd
  return `${sanitized}_${date}.json`;
}

/** Deck-scoped export modal: shows what will be downloaded and triggers the file download (US-E1). */
export default function ExportModal({ deck, onClose, onExported }: ExportModalProps) {
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const dialogRef = useRef<HTMLDivElement>(null);
  const downloadRef = useRef<HTMLButtonElement>(null);
  useFocusTrap(dialogRef, true, onClose, downloadRef);

  const filename = suggestedFilename(deck.name);

  const handleDownload = async () => {
    setDownloading(true);
    setError(null);
    try {
      await apiClient.download(`/api/decks/${deck.id}/export`, filename);
      onExported?.(deck);
      onClose();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="export-modal-title"
        data-testid="export-modal"
        ref={dialogRef}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="export-modal-title">Export Deck</h2>

        <div className="export-summary">
          <p>
            <span
              className="deck-color-dot"
              style={{ background: deck.color }}
              aria-hidden="true"
            />
            <strong>{deck.name}</strong>
          </p>
          <p className="muted" data-testid="export-card-count">
            {deck.totalCards} cards
          </p>
          <p className="muted">
            Saves to <code data-testid="export-filename">{filename}</code>
          </p>
          <p className="muted">
            Includes all card content, SRS progress, learning states, and statistics.
          </p>
        </div>

        {error && (
          <p className="error" role="alert" data-testid="export-error">
            {error}
          </p>
        )}

        <div className="modal-actions">
          <button
            type="button"
            className="button-secondary"
            data-testid="export-cancel"
            onClick={onClose}
          >
            Cancel
          </button>
          <button
            type="button"
            ref={downloadRef}
            data-testid="export-download-btn"
            onClick={handleDownload}
            disabled={downloading}
          >
            {downloading ? 'Downloading…' : '⬇ Download'}
          </button>
        </div>
      </div>
    </div>
  );
}
