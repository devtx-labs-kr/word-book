package com.wordbook.service;

import com.wordbook.domain.Card;
import com.wordbook.domain.Deck;
import com.wordbook.domain.LearningState;
import com.wordbook.exception.NotFoundException;
import com.wordbook.repository.CardRepository;
import com.wordbook.repository.DeckRepository;
import com.wordbook.web.dto.CreateCardRequest;
import com.wordbook.web.dto.UpdateCardRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Card lifecycle: list/filter/search + full CRUD (FR-2). New cards receive SM-2 defaults
 * (state=NEW, EF=2.5, interval=0, nextReviewDate=now) from the {@link Card} entity; the update path
 * never touches SRS fields (DR-5).
 */
@Service
public class CardService {

  private final CardRepository cardRepository;
  private final DeckRepository deckRepository;

  public CardService(CardRepository cardRepository, DeckRepository deckRepository) {
    this.cardRepository = cardRepository;
    this.deckRepository = deckRepository;
  }

  /**
   * Lists a deck's cards, applying a learning-state {@code filter} (All/New/Learning/Mastered/Due)
   * and a case-insensitive front/back {@code search}, AND-combined in the service layer (SR-3/SR-4/
   * SR-5). {@code Due} = nextReviewDate &le; now.
   */
  @Transactional(readOnly = true)
  public List<Card> listByDeck(UUID deckId, String filter, String search) {
    // Verify the deck exists (ER-1 -> 404).
    if (!deckRepository.existsById(deckId)) {
      throw new NotFoundException("Deck not found: " + deckId);
    }
    List<Card> cards = cardRepository.findByDeckId(deckId);
    String f = filter == null ? "all" : filter.trim().toLowerCase();
    Instant now = Instant.now();
    String q = search == null ? null : search.trim().toLowerCase();

    return cards.stream()
        .filter(c -> matchesFilter(c, f, now))
        .filter(c -> matchesSearch(c, q))
        .collect(Collectors.toList());
  }

  private boolean matchesFilter(Card card, String filter, Instant now) {
    return switch (filter) {
      case "new" -> card.getLearningState() == LearningState.NEW;
      case "learning" -> card.getLearningState() == LearningState.LEARNING;
      case "mastered" -> card.getLearningState() == LearningState.MASTERED;
      case "due" -> !card.getNextReviewDate().isAfter(now);
      default -> true; // "all" or unrecognized
    };
  }

  private boolean matchesSearch(Card card, String query) {
    if (query == null || query.isEmpty()) {
      return true;
    }
    String front = card.getFront() == null ? "" : card.getFront().toLowerCase();
    String back = card.getBack() == null ? "" : card.getBack().toLowerCase();
    return front.contains(query) || back.contains(query);
  }

  @Transactional
  public Card create(UUID deckId, CreateCardRequest request) {
    Deck deck =
        deckRepository
            .findById(deckId)
            .orElseThrow(() -> new NotFoundException("Deck not found: " + deckId));
    Card card = new Card(request.getFront(), request.getBack());
    card.setExample(request.getExample());
    card.setPronunciation(request.getPronunciation());
    card.setNotes(request.getNotes());
    card.setDeck(deck);
    return cardRepository.save(card);
  }

  /** Convenience overload for front/back-only creation (U1 walking-skeleton slice). */
  @Transactional
  public Card create(UUID deckId, String front, String back) {
    CreateCardRequest request = new CreateCardRequest();
    request.setFront(front);
    request.setBack(back);
    return create(deckId, request);
  }

  @Transactional(readOnly = true)
  public Card get(UUID id) {
    return cardRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Card not found: " + id));
  }

  /**
   * Updates a card's display fields (front/back/example/pronunciation/notes) and bumps {@code
   * updatedAt}. SRS fields (learningState/easeFactor/interval/repetitions/nextReviewDate and the
   * review statistics) are left untouched (DR-5).
   */
  @Transactional
  public Card update(UUID id, UpdateCardRequest request) {
    Card card = get(id);
    card.setFront(request.getFront());
    card.setBack(request.getBack());
    card.setExample(request.getExample());
    card.setPronunciation(request.getPronunciation());
    card.setNotes(request.getNotes());
    card.setUpdatedAt(Instant.now());
    return cardRepository.save(card);
  }

  /** Deletes a card; its review records cascade away (DR-2). */
  @Transactional
  public void delete(UUID id) {
    Card card = get(id);
    cardRepository.delete(card);
  }
}
