import { useState } from 'react';
import { DeckSummaryResponse } from '../api/types';

interface DeckCardProps {
  deck: DeckSummaryResponse;
  onOpen: (deck: DeckSummaryResponse) => void;
  onEdit: (deck: DeckSummaryResponse) => void;
  onDelete: (deck: DeckSummaryResponse) => void;
  onStudy: (deck: DeckSummaryResponse) => void;
  onExport: (deck: DeckSummaryResponse) => void;
}

/** A single deck row in the deck list: color dot + name + stat pills + Study + a ⋯ action menu. */
export default function DeckCard({
  deck,
  onOpen,
  onEdit,
  onDelete,
  onStudy,
  onExport,
}: DeckCardProps) {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <li className="deck-card" data-testid="deck-card">
      <div
        className="deck-card-main"
        role="button"
        tabIndex={0}
        data-testid="deck-card-open"
        onClick={() => onOpen(deck)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onOpen(deck);
          }
        }}
      >
        <span className="deck-color-dot" style={{ background: deck.color }} aria-hidden="true" />
        <div className="deck-card-info">
          <span className="deck-name">{deck.name}</span>
          <span className="deck-meta">
            <span data-testid="deck-total">{deck.totalCards} cards</span>
            <span className="deck-meta-sep" aria-hidden="true">
              ·
            </span>
            <span data-testid="deck-due">{deck.dueCards} due</span>
            <span className="deck-meta-sep" aria-hidden="true">
              ·
            </span>
            <span data-testid="deck-new">{deck.newCards} new</span>
          </span>
        </div>
      </div>
      <div className="deck-card-actions">
        {deck.dueCards > 0 && (
          <span className="deck-due-badge" aria-hidden="true">
            {deck.dueCards} due
          </span>
        )}
        <button
          type="button"
          data-testid="deck-study"
          onClick={() => onStudy(deck)}
          aria-label={`Study ${deck.name}`}
        >
          ▶ Study
        </button>
        <div className="menu-wrapper">
          <button
            type="button"
            className="button-icon"
            data-testid="deck-menu-toggle"
            aria-haspopup="true"
            aria-expanded={menuOpen}
            aria-label={`Actions for ${deck.name}`}
            onClick={() => setMenuOpen((o) => !o)}
          >
            ⋯
          </button>
          {menuOpen && (
            <ul className="menu" role="menu" data-testid="deck-menu">
              <li role="none">
                <button
                  type="button"
                  role="menuitem"
                  data-testid="deck-edit"
                  onClick={() => {
                    setMenuOpen(false);
                    onEdit(deck);
                  }}
                >
                  Edit
                </button>
              </li>
              <li role="none">
                <button
                  type="button"
                  role="menuitem"
                  data-testid="deck-export-action"
                  onClick={() => {
                    setMenuOpen(false);
                    onExport(deck);
                  }}
                >
                  Export
                </button>
              </li>
              <li role="none">
                <button
                  type="button"
                  role="menuitem"
                  className="menu-danger"
                  data-testid="deck-delete"
                  onClick={() => {
                    setMenuOpen(false);
                    onDelete(deck);
                  }}
                >
                  Delete
                </button>
              </li>
            </ul>
          )}
        </div>
      </div>
    </li>
  );
}
