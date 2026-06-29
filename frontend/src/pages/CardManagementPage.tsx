import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { apiClient } from '../api/apiClient';
import { CardFilter, CardRequest, CardResponse, DeckResponse } from '../api/types';
import { useDebounce } from '../hooks/useDebounce';
import CardRow from '../components/CardRow';
import CardFormModal from '../components/CardFormModal';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';

const FILTERS: CardFilter[] = ['All', 'New', 'Learning', 'Mastered', 'Due'];

/** Card management for a single deck: list, filter tabs, search, add/edit/delete (FR-2, US-B1~B3). */
export default function CardManagementPage() {
  const { id: deckId } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [deck, setDeck] = useState<DeckResponse | null>(null);
  const [cards, setCards] = useState<CardResponse[]>([]);
  const [filter, setFilter] = useState<CardFilter>('All');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingCard, setEditingCard] = useState<CardResponse | null>(null);
  const [deletingCard, setDeletingCard] = useState<CardResponse | null>(null);

  const debouncedSearch = useDebounce(search, 250);

  const loadDeck = useCallback(async () => {
    if (!deckId) return;
    try {
      const result = await apiClient.get<DeckResponse>(`/api/decks/${deckId}`);
      setDeck(result);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }, [deckId]);

  const loadCards = useCallback(async () => {
    if (!deckId) return;
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.set('filter', filter);
      if (debouncedSearch.trim()) params.set('search', debouncedSearch.trim());
      const result = await apiClient.get<CardResponse[]>(
        `/api/decks/${deckId}/cards?${params.toString()}`,
      );
      setCards(result);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [deckId, filter, debouncedSearch]);

  useEffect(() => {
    void loadDeck();
  }, [loadDeck]);

  useEffect(() => {
    void loadCards();
  }, [loadCards]);

  const handleAdd = async (payload: CardRequest) => {
    if (!deckId) return;
    setError(null);
    try {
      await apiClient.post(`/api/decks/${deckId}/cards`, payload);
      setIsAddOpen(false);
      await Promise.all([loadCards(), loadDeck()]);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  const handleEdit = async (payload: CardRequest) => {
    if (!editingCard) return;
    setError(null);
    try {
      await apiClient.put(`/api/cards/${editingCard.id}`, payload);
      setEditingCard(null);
      await loadCards();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  const handleDelete = async () => {
    if (!deletingCard) return;
    setError(null);
    try {
      await apiClient.delete(`/api/cards/${deletingCard.id}`);
      setDeletingCard(null);
      await Promise.all([loadCards(), loadDeck()]);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  const isFiltering = debouncedSearch.trim() !== '' || filter !== 'All';

  return (
    <section data-testid="card-management">
      <header className="page-header">
        <button
          className="button-link"
          data-testid="back-to-decks"
          onClick={() => navigate('/decks')}
        >
          ← Decks
        </button>
        <h1>{deck?.name ?? 'Deck'}</h1>
        <div className="page-header-actions">
          <button
            data-testid="study-deck-button"
            onClick={() => deckId && navigate(`/study/${deckId}`)}
          >
            ▶ Study
          </button>
          <button data-testid="add-card-button" onClick={() => setIsAddOpen(true)}>
            ＋ Add Card
          </button>
        </div>
      </header>

      <div className="toolbar">
        <input
          type="search"
          role="searchbox"
          aria-label="Search cards"
          placeholder="🔍  Search cards…"
          data-testid="card-search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <div role="tablist" aria-label="Filter cards by state" className="filter-tabs">
        {FILTERS.map((f) => (
          <button
            key={f}
            role="tab"
            type="button"
            aria-selected={filter === f}
            tabIndex={filter === f ? 0 : -1}
            className={`filter-tab${filter === f ? ' active' : ''}`}
            data-testid={`filter-${f.toLowerCase()}`}
            onClick={() => setFilter(f)}
            onKeyDown={(e) => {
              if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
                e.preventDefault();
                const idx = FILTERS.indexOf(filter);
                const next =
                  e.key === 'ArrowRight'
                    ? FILTERS[(idx + 1) % FILTERS.length]
                    : FILTERS[(idx - 1 + FILTERS.length) % FILTERS.length];
                setFilter(next);
              }
            }}
          >
            {f}
          </button>
        ))}
      </div>

      {error && (
        <p className="error" role="alert" data-testid="error-message">
          {error}
        </p>
      )}

      {loading ? (
        <p data-testid="loading">Loading…</p>
      ) : cards.length === 0 ? (
        isFiltering ? (
          <EmptyState
            icon="🔍"
            title="No cards match"
            description="Try a different search or filter."
          />
        ) : (
          <EmptyState
            icon="🗂"
            title="No cards yet"
            description="Add your first card to this deck."
            action={
              <button data-testid="empty-add-card" onClick={() => setIsAddOpen(true)}>
                ＋ Add Card
              </button>
            }
          />
        )
      ) : (
        <ul className="card-list" role="list">
          {cards.map((card) => (
            <CardRow
              key={card.id}
              card={card}
              onEdit={(c) => setEditingCard(c)}
              onDelete={(c) => setDeletingCard(c)}
            />
          ))}
        </ul>
      )}

      {isAddOpen && deckId && (
        <CardFormModal mode="add" onSubmit={handleAdd} onClose={() => setIsAddOpen(false)} />
      )}
      {editingCard && (
        <CardFormModal
          mode="edit"
          initial={editingCard}
          onSubmit={handleEdit}
          onClose={() => setEditingCard(null)}
        />
      )}
      {deletingCard && (
        <ConfirmDialog
          title="Delete card?"
          message={`Delete "${deletingCard.front}"? This also removes its review history and cannot be undone.`}
          confirmLabel="Delete"
          onConfirm={handleDelete}
          onCancel={() => setDeletingCard(null)}
        />
      )}
    </section>
  );
}
