package com.wordbook.service;

import com.wordbook.domain.Deck;
import com.wordbook.exception.NotFoundException;
import com.wordbook.repository.DeckRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deck lifecycle: list/search/sort + full CRUD (FR-1). */
@Service
public class DeckService {

  private final DeckRepository deckRepository;

  public DeckService(DeckRepository deckRepository) {
    this.deckRepository = deckRepository;
  }

  /**
   * Lists decks, optionally filtered by a case-insensitive name substring and sorted by {@code
   * name} (default), {@code createdAt}, or {@code due} (due-card count, descending). The {@code
   * due} sort is derived in memory; unrecognized sort values fall back to {@code name} (SR-1/SR-2).
   */
  @Transactional(readOnly = true)
  public List<Deck> list(String search, String sort) {
    String key = sort == null ? "name" : sort.trim().toLowerCase();
    boolean hasSearch = search != null && !search.isBlank();

    if ("due".equals(key)) {
      // Derived value -> in-memory sort (NFR-9, single-user scale).
      List<Deck> decks =
          hasSearch
              ? deckRepository.findByNameContainingIgnoreCase(search)
              : deckRepository.findAll();
      decks.sort(Comparator.comparingLong(Deck::getDueCards).reversed());
      return initStats(decks);
    }

    Sort dbSort =
        switch (key) {
          case "createdat" -> Sort.by(Sort.Direction.ASC, "createdAt");
          default -> Sort.by(Sort.Direction.ASC, "name");
        };
    List<Deck> decks =
        hasSearch
            ? deckRepository.findByNameContainingIgnoreCase(search, dbSort)
            : deckRepository.findAll(dbSort);
    return initStats(decks);
  }

  /**
   * Eagerly initializes each deck's lazy {@code cards} collection while the read transaction is
   * open, so the derived stat getters (totalCards/dueCards/newCards) stay usable when the
   * controller maps the entities after the transaction closes (open-in-view is off). Acceptable at
   * single-user scale (logical-components review finding #3).
   */
  private List<Deck> initStats(List<Deck> decks) {
    decks.forEach(d -> d.getCards().size());
    return decks;
  }

  @Transactional
  public Deck create(String name, String description, String color) {
    Deck deck = new Deck(name, description, color);
    return deckRepository.save(deck);
  }

  @Transactional(readOnly = true)
  public Deck get(UUID id) {
    Deck deck =
        deckRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Deck not found: " + id));
    deck.getCards().size(); // init stats while the session is open
    return deck;
  }

  /** Updates a deck's editable fields and bumps {@code updatedAt} (DR-4). */
  @Transactional
  public Deck update(UUID id, String name, String description, String color) {
    Deck deck = get(id);
    deck.setName(name);
    if (description != null) {
      deck.setDescription(description);
    }
    if (color != null) {
      deck.setColor(color);
    }
    deck.setUpdatedAt(Instant.now());
    return deckRepository.save(deck);
  }

  /** Deletes a deck; cards and their review records cascade away (DR-1). */
  @Transactional
  public void delete(UUID id) {
    Deck deck = get(id);
    deckRepository.delete(deck);
  }

  @Transactional
  public Deck touch(Deck deck) {
    deck.setUpdatedAt(Instant.now());
    return deckRepository.save(deck);
  }
}
