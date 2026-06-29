package com.wordbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wordbook.domain.Card;
import com.wordbook.domain.Deck;
import com.wordbook.domain.LearningState;
import com.wordbook.domain.ReviewRating;
import com.wordbook.domain.ReviewRecord;
import com.wordbook.exception.NotFoundException;
import com.wordbook.repository.CardRepository;
import com.wordbook.repository.ReviewRecordRepository;
import com.wordbook.web.dto.CreateCardRequest;
import com.wordbook.web.dto.UpdateCardRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Service-level tests for card list/filter/search + create/update/delete (FR-2, SR-3/SR-4/SR-5,
 * DR-2/DR-5).
 */
@SpringBootTest
class CardServiceTest {

  @Autowired private DeckService deckService;
  @Autowired private CardService cardService;
  @Autowired private CardRepository cardRepository;
  @Autowired private ReviewRecordRepository reviewRecordRepository;

  private CreateCardRequest createRequest(String front, String back) {
    CreateCardRequest req = new CreateCardRequest();
    req.setFront(front);
    req.setBack(back);
    return req;
  }

  @Test
  void newCardGetsEntityDefaults() {
    Deck deck = deckService.create("Defaults-" + UUID.randomUUID(), null, null);
    CreateCardRequest req = createRequest("apple", "사과");
    req.setPronunciation("/ˈæp.əl/");
    req.setExample("an apple a day");
    req.setNotes("fruit");

    Card card = cardService.create(deck.getId(), req);

    assertThat(card.getLearningState()).isEqualTo(LearningState.NEW);
    assertThat(card.getEaseFactor()).isEqualTo(2.5);
    assertThat(card.getInterval()).isEqualTo(0);
    assertThat(card.getRepetitions()).isEqualTo(0);
    assertThat(card.getTotalReviews()).isEqualTo(0);
    assertThat(card.getPronunciation()).isEqualTo("/ˈæp.əl/");
    assertThat(card.getExample()).isEqualTo("an apple a day");
    assertThat(card.getNotes()).isEqualTo("fruit");
  }

  @Test
  void filterAndSearchAreAndCombined() {
    Deck deck = deckService.create("FilterSearch-" + UUID.randomUUID(), null, null);
    // NEW + matches "app"
    cardService.create(deck.getId(), createRequest("apple", "사과"));
    // NEW but no "app" match
    cardService.create(deck.getId(), createRequest("banana", "바나나"));
    // matches "app" but state MASTERED
    Card mastered = cardService.create(deck.getId(), createRequest("application", "응용"));
    mastered.setLearningState(LearningState.MASTERED);
    cardRepository.save(mastered);

    List<Card> result = cardService.listByDeck(deck.getId(), "New", "app");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getFront()).isEqualTo("apple");
  }

  @Test
  void dueFilterSelectsCardsDueNowOrEarlier() {
    Deck deck = deckService.create("Due-" + UUID.randomUUID(), null, null);
    Card due = cardService.create(deck.getId(), createRequest("due", "due"));
    due.setNextReviewDate(Instant.now().minus(1, ChronoUnit.DAYS));
    cardRepository.save(due);
    Card future = cardService.create(deck.getId(), createRequest("future", "future"));
    future.setNextReviewDate(Instant.now().plus(10, ChronoUnit.DAYS));
    cardRepository.save(future);

    List<Card> result = cardService.listByDeck(deck.getId(), "Due", null);

    assertThat(result).extracting(Card::getFront).containsExactly("due");
  }

  @Test
  void searchMatchesFrontOrBackCaseInsensitively() {
    Deck deck = deckService.create("BackSearch-" + UUID.randomUUID(), null, null);
    cardService.create(deck.getId(), createRequest("hello", "안녕 WORLD"));
    cardService.create(deck.getId(), createRequest("bye", "잘가"));

    List<Card> result = cardService.listByDeck(deck.getId(), "All", "world");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getFront()).isEqualTo("hello");
  }

  @Test
  void listUnknownDeckThrowsNotFound() {
    assertThatThrownBy(() -> cardService.listByDeck(UUID.randomUUID(), "All", null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void updateChangesDisplayFieldsButNotSrsState() {
    Deck deck = deckService.create("EditSrs-" + UUID.randomUUID(), null, null);
    Card card = cardService.create(deck.getId(), createRequest("old", "옛날"));
    // Simulate a card that has been studied (non-default SRS state).
    card.setLearningState(LearningState.MASTERED);
    card.setEaseFactor(2.8);
    card.setInterval(15);
    card.setRepetitions(4);
    card.setNextReviewDate(Instant.now().plus(15, ChronoUnit.DAYS));
    card.setTotalReviews(5);
    card.setCorrectReviews(4);
    Card studied = cardRepository.save(card);
    Instant lockedNextReview = studied.getNextReviewDate();

    UpdateCardRequest req = new UpdateCardRequest();
    req.setFront("new");
    req.setBack("새것");
    req.setPronunciation("/njuː/");
    Card updated = cardService.update(studied.getId(), req);

    // Display fields changed.
    assertThat(updated.getFront()).isEqualTo("new");
    assertThat(updated.getBack()).isEqualTo("새것");
    assertThat(updated.getPronunciation()).isEqualTo("/njuː/");
    // SRS fields unchanged (DR-5).
    assertThat(updated.getLearningState()).isEqualTo(LearningState.MASTERED);
    assertThat(updated.getEaseFactor()).isEqualTo(2.8);
    assertThat(updated.getInterval()).isEqualTo(15);
    assertThat(updated.getRepetitions()).isEqualTo(4);
    assertThat(updated.getTotalReviews()).isEqualTo(5);
    assertThat(updated.getCorrectReviews()).isEqualTo(4);
    assertThat(updated.getNextReviewDate()).isEqualTo(lockedNextReview);
  }

  @Test
  void deleteCardCascadesReviewRecords() {
    Deck deck = deckService.create("Cascade-" + UUID.randomUUID(), null, null);
    Card card = cardService.create(deck.getId(), createRequest("word", "단어"));
    ReviewRecord record =
        reviewRecordRepository.save(new ReviewRecord(card, ReviewRating.GOOD, 0, 1, 2.5, 2.5, 1.5));
    UUID cardId = card.getId();
    UUID recordId = record.getId();
    assertThat(reviewRecordRepository.findById(recordId)).isPresent();

    cardService.delete(cardId);

    assertThat(cardRepository.findById(cardId)).isEmpty();
    assertThat(reviewRecordRepository.findById(recordId)).isEmpty();
  }
}
