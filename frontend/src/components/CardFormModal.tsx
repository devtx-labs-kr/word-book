import { FormEvent, useRef, useState } from 'react';
import { CardRequest, CardResponse } from '../api/types';
import { useFocusTrap } from '../hooks/useFocusTrap';

interface CardFormModalProps {
  mode: 'add' | 'edit';
  initial?: CardResponse;
  onSubmit: (payload: CardRequest) => void;
  onClose: () => void;
}

/** Add/edit card modal including the pronunciation field (FR-2.1). Focus trap + Esc to close. */
export default function CardFormModal({ mode, initial, onSubmit, onClose }: CardFormModalProps) {
  const [front, setFront] = useState(initial?.front ?? '');
  const [back, setBack] = useState(initial?.back ?? '');
  const [example, setExample] = useState(initial?.example ?? '');
  const [pronunciation, setPronunciation] = useState(initial?.pronunciation ?? '');
  const [notes, setNotes] = useState(initial?.notes ?? '');

  const dialogRef = useRef<HTMLDivElement>(null);
  const frontRef = useRef<HTMLInputElement>(null);
  useFocusTrap(dialogRef, true, onClose, frontRef);

  const valid = front.trim() !== '' && back.trim() !== '';

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!valid) return;
    onSubmit({
      front: front.trim(),
      back: back.trim(),
      example: example.trim() || undefined,
      pronunciation: pronunciation.trim() || undefined,
      notes: notes.trim() || undefined,
    });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="card-form-title"
        data-testid="card-form-modal"
        ref={dialogRef}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="card-form-title">{mode === 'add' ? 'Add Card' : 'Edit Card'}</h2>
        <form onSubmit={handleSubmit}>
          <label htmlFor="card-front">Word</label>
          <input
            id="card-front"
            data-testid="card-front-input"
            value={front}
            ref={frontRef}
            onChange={(e) => setFront(e.target.value)}
            aria-invalid={front.trim() === ''}
            required
          />

          <label htmlFor="card-back">Definition</label>
          <input
            id="card-back"
            data-testid="card-back-input"
            value={back}
            onChange={(e) => setBack(e.target.value)}
            aria-invalid={back.trim() === ''}
            required
          />
          {!valid && (
            <p className="field-error" data-testid="card-form-error">
              Word and Definition are required.
            </p>
          )}

          <label htmlFor="card-pronunciation">Pronunciation</label>
          <input
            id="card-pronunciation"
            data-testid="card-pronunciation-input"
            value={pronunciation}
            onChange={(e) => setPronunciation(e.target.value)}
          />

          <label htmlFor="card-example">Example</label>
          <input
            id="card-example"
            data-testid="card-example-input"
            value={example}
            onChange={(e) => setExample(e.target.value)}
          />

          <label htmlFor="card-notes">Notes</label>
          <input
            id="card-notes"
            data-testid="card-notes-input"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />

          <div className="modal-actions">
            <button
              type="button"
              className="button-secondary"
              data-testid="card-form-cancel"
              onClick={onClose}
            >
              Cancel
            </button>
            <button type="submit" data-testid="card-form-submit" disabled={!valid}>
              {mode === 'add' ? 'Add' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
