import { ChangeEvent, useRef, useState } from 'react';
import { apiClient } from '../api/apiClient';
import { DeckResponse, ImportPreview } from '../api/types';
import { useFocusTrap } from '../hooks/useFocusTrap';

interface ImportModalProps {
  onClose: () => void;
  /** Called with the created deck after a successful import (refresh list + success toast). */
  onImported: (deck: DeckResponse) => void;
}

/**
 * Page-level import modal with the two-step flow (US-E2): pick a .json file -&gt; preview validation
 * -&gt; confirm import. Preview and import send the same file text as a raw application/json body.
 */
export default function ImportModal({ onClose, onImported }: ImportModalProps) {
  const [fileText, setFileText] = useState<string | null>(null);
  const [preview, setPreview] = useState<ImportPreview | null>(null);
  const [previewing, setPreviewing] = useState(false);
  const [importing, setImporting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const dialogRef = useRef<HTMLDivElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  useFocusTrap(dialogRef, true, onClose, fileRef);

  const handleFileChange = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    setPreview(null);
    setError(null);
    setFileText(null);
    if (!file) return;

    setPreviewing(true);
    try {
      const text = await file.text();
      setFileText(text);
      const result = await apiClient.postRaw<ImportPreview>('/api/decks/import/preview', text);
      setPreview(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setPreviewing(false);
    }
  };

  const handleImport = async () => {
    if (!fileText || !preview?.valid) return;
    setImporting(true);
    setError(null);
    try {
      const deck = await apiClient.postRaw<DeckResponse>('/api/decks/import', fileText);
      onImported(deck);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setImporting(false);
    }
  };

  const canImport = Boolean(preview?.valid) && !importing;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="import-modal-title"
        data-testid="import-modal"
        ref={dialogRef}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="import-modal-title">Import Deck</h2>

        <label htmlFor="import-file">Choose a WordBook .json file</label>
        <input
          id="import-file"
          type="file"
          accept="application/json,.json"
          ref={fileRef}
          data-testid="import-file-input"
          onChange={handleFileChange}
        />

        <div aria-live="polite" data-testid="import-preview">
          {previewing && <p className="muted">Checking file…</p>}
          {preview?.valid && (
            <div className="import-preview-ok">
              <p>
                <strong>{preview.deckName}</strong> — {preview.cardCount} cards
              </p>
              {preview.duplicateName && (
                <p className="muted" data-testid="import-duplicate-note">
                  A deck with this name already exists, so it will be saved as{' '}
                  <strong>{preview.duplicateName}</strong>.
                </p>
              )}
            </div>
          )}
          {preview && !preview.valid && (
            <p className="error" role="alert" data-testid="import-invalid">
              That file isn’t a valid WordBook export. Please choose another file.
            </p>
          )}
        </div>

        {error && (
          <p className="error" role="alert" data-testid="import-error">
            {error}
          </p>
        )}

        <div className="modal-actions">
          <button
            type="button"
            className="button-secondary"
            data-testid="import-cancel"
            onClick={onClose}
          >
            Cancel
          </button>
          <button
            type="button"
            data-testid="import-confirm-btn"
            onClick={handleImport}
            disabled={!canImport}
            aria-disabled={!canImport}
          >
            {importing ? 'Importing…' : 'Import'}
          </button>
        </div>
      </div>
    </div>
  );
}
