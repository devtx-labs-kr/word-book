import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../api/apiClient';
import { DeckRequest, DeckResponse, DeckSort, DeckSummaryResponse } from '../api/types';
import { useDebounce } from '../hooks/useDebounce';
import DeckCard from '../components/DeckCard';
import DeckFormModal from '../components/DeckFormModal';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';
import ExportModal from '../components/ExportModal';
import ImportModal from '../components/ImportModal';

/** Deck list with search, sort, and create/edit/delete (FR-1, US-A1~A4). */
export default function DeckListPage() {
  const navigate = useNavigate();
  const [decks, setDecks] = useState<DeckSummaryResponse[]>([]);
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState<DeckSort>('name');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingDeck, setEditingDeck] = useState<DeckSummaryResponse | null>(null);
  const [deletingDeck, setDeletingDeck] = useState<DeckSummaryResponse | null>(null);
  const [exportingDeck, setExportingDeck] = useState<DeckSummaryResponse | null>(null);
  const [isImportOpen, setIsImportOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const debouncedSearch = useDebounce(search, 250);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (debouncedSearch.trim()) params.set('search', debouncedSearch.trim());
      params.set('sort', sort);
      const result = await apiClient.get<DeckSummaryResponse[]>(`/api/decks?${params.toString()}`);
      setDecks(result);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, sort]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 3000);
    return () => clearTimeout(timer);
  }, [toast]);

  const handleCreate = async (payload: DeckRequest) => {
    setError(null);
    try {
      await apiClient.post('/api/decks', payload);
      setIsCreateOpen(false);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  const handleEdit = async (payload: DeckRequest) => {
    if (!editingDeck) return;
    setError(null);
    try {
      await apiClient.put(`/api/decks/${editingDeck.id}`, payload);
      setEditingDeck(null);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  const handleDelete = async () => {
    if (!deletingDeck) return;
    setError(null);
    try {
      await apiClient.delete(`/api/decks/${deletingDeck.id}`);
      setDeletingDeck(null);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  const handleImported = async (deck: DeckResponse) => {
    setIsImportOpen(false);
    setToast(`Imported "${deck.name}".`);
    await load();
  };

  const isSearching = debouncedSearch.trim() !== '';

  return (
    <section data-testid="deck-list">
      <header className="page-header">
        <h1>My Decks</h1>
        <div className="page-header-actions">
          <button
            className="button-secondary"
            data-testid="deck-import-btn"
            onClick={() => setIsImportOpen(true)}
          >
            ⬇ Import
          </button>
          <button data-testid="new-deck-button" onClick={() => setIsCreateOpen(true)}>
            ＋ New Deck
          </button>
        </div>
      </header>

      <div className="toolbar">
        <input
          type="search"
          role="searchbox"
          aria-label="Search decks"
          placeholder="🔍  Search decks…"
          data-testid="deck-search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <label htmlFor="deck-sort" className="visually-hidden">
          Sort decks
        </label>
        <select
          id="deck-sort"
          data-testid="deck-sort"
          value={sort}
          onChange={(e) => setSort(e.target.value as DeckSort)}
        >
          <option value="name">Name</option>
          <option value="createdAt">Created</option>
          <option value="due">Due</option>
        </select>
      </div>

      {error && (
        <p className="error" role="alert" data-testid="error-message">
          {error}
        </p>
      )}

      {toast && (
        <p className="toast" role="status" data-testid="toast">
          {toast}
        </p>
      )}

      {loading ? (
        <p data-testid="loading">Loading…</p>
      ) : decks.length === 0 ? (
        isSearching ? (
          <EmptyState
            icon="🔍"
            title="No decks match your search"
            description="Try a different search term."
          />
        ) : (
          <EmptyState
            icon="📚"
            title="No decks yet"
            description="Create your first deck to start adding cards."
            action={
              <button data-testid="empty-create-deck" onClick={() => setIsCreateOpen(true)}>
                ＋ New Deck
              </button>
            }
          />
        )
      ) : (
        <ul className="deck-grid" role="list">
          {decks.map((deck) => (
            <DeckCard
              key={deck.id}
              deck={deck}
              onOpen={(d) => navigate(`/decks/${d.id}`)}
              onStudy={(d) => navigate(`/study/${d.id}`)}
              onEdit={(d) => setEditingDeck(d)}
              onDelete={(d) => setDeletingDeck(d)}
              onExport={(d) => setExportingDeck(d)}
            />
          ))}
        </ul>
      )}

      {isCreateOpen && (
        <DeckFormModal
          mode="create"
          onSubmit={handleCreate}
          onClose={() => setIsCreateOpen(false)}
        />
      )}
      {editingDeck && (
        <DeckFormModal
          mode="edit"
          initial={editingDeck}
          onSubmit={handleEdit}
          onClose={() => setEditingDeck(null)}
        />
      )}
      {deletingDeck && (
        <ConfirmDialog
          title="Delete deck?"
          message={`Deleting "${deletingDeck.name}" also removes all its cards and review history. This cannot be undone.`}
          confirmLabel="Delete"
          onConfirm={handleDelete}
          onCancel={() => setDeletingDeck(null)}
        />
      )}
      {exportingDeck && (
        <ExportModal
          deck={exportingDeck}
          onExported={(d) => setToast(`Exported "${d.name}".`)}
          onClose={() => setExportingDeck(null)}
        />
      )}
      {isImportOpen && (
        <ImportModal onImported={handleImported} onClose={() => setIsImportOpen(false)} />
      )}
    </section>
  );
}
