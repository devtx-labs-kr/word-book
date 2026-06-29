package com.wordbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wordbook.domain.Card;
import com.wordbook.domain.Deck;
import com.wordbook.domain.LearningState;
import com.wordbook.domain.ReviewRating;
import com.wordbook.exception.NotFoundException;
import com.wordbook.repository.CardRepository;
import com.wordbook.repository.ReviewRecordRepository;
import com.wordbook.repository.StudySessionRepository;
import com.wordbook.web.dto.SessionSummary;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Service-level tests for the study slice: defaults, the submitAnswer transaction, error paths. */
@SpringBootTest
class StudySessionServiceTest {

  @Autowired private DeckService deckService;
  @Autowired private CardService cardService;
  @Autowired private StudySessionService studySessionService;
  @Autowired private CardRepository cardRepository;
  @Autowired private ReviewRecordRepository reviewRecordRepository;
  @Autowired private StudySessionRepository studySessionRepository;

  @Test
  void newCardGetsSm2Defaults() {
    Deck deck = deckService.create("Defaults", null, null);
    Card card = cardService.create(deck.getId(), "front", "back");
    assertThat(card.getLearningState()).isEqualTo(LearningState.NEW);
    assertThat(card.getEaseFactor()).isEqualTo(2.5);
    assertThat(card.getInterval()).isEqualTo(0);
    assertThat(card.getRepetitions()).isEqualTo(0);
    assertThat(card.getNextReviewDate()).isNotNull();
  }

  @Test
  void submitAnswerRunsFullTransaction() {
    Deck deck = deckService.create("Study", null, null);
    Card card = cardService.create(deck.getId(), "apple", "사과");
    Instant originalNextReview = card.getNextReviewDate();

    StudySessionStart start = studySessionService.start(deck.getId());
    assertThat(start.cards()).hasSize(1);

    long recordsBefore = reviewRecordRepository.count();
    Card updated =
        studySessionService.submitAnswer(
            start.session().getId(), card.getId(), ReviewRating.GOOD, 4200L);

    // Card SRS fields updated (GOOD, rep0 -> interval 1, rep -> 1).
    assertThat(updated.getInterval()).isEqualTo(1);
    assertThat(updated.getRepetitions()).isEqualTo(1);
    assertThat(updated.getTotalReviews()).isEqualTo(1);
    assertThat(updated.getCorrectReviews()).isEqualTo(1);
    assertThat(updated.getLastReviewedAt()).isNotNull();
    assertThat(updated.getNextReviewDate()).isAfter(originalNextReview);

    // ReviewRecord written with timeSpent in seconds (4200ms -> 4.2s).
    assertThat(reviewRecordRepository.count()).isEqualTo(recordsBefore + 1);
    var record =
        reviewRecordRepository.findAll().stream()
            .filter(r -> r.getCard().getId().equals(card.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(record.getTimeSpent()).isEqualTo(4.2);
    assertThat(record.getRating()).isEqualTo(ReviewRating.GOOD);
  }

  @Test
  void againResetsRepetitionsAndDoesNotCountCorrect() {
    Deck deck = deckService.create("Again", null, null);
    Card card = cardService.create(deck.getId(), "front", "back");
    card.setRepetitions(3);
    cardRepository.save(card);

    StudySessionStart start = studySessionService.start(deck.getId());
    Card updated =
        studySessionService.submitAnswer(
            start.session().getId(), card.getId(), ReviewRating.AGAIN, 1000L);

    assertThat(updated.getRepetitions()).isEqualTo(0);
    assertThat(updated.getLearningState()).isEqualTo(LearningState.RELEARNING);
    assertThat(updated.getInterval()).isEqualTo(1);
    assertThat(updated.getCorrectReviews()).isEqualTo(0);
  }

  @Test
  void startUnknownDeckThrowsNotFound() {
    assertThatThrownBy(() -> studySessionService.start(UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void submitAnswerUnknownSessionThrowsNotFound() {
    Deck deck = deckService.create("X", null, null);
    Card card = cardService.create(deck.getId(), "f", "b");
    assertThatThrownBy(
            () ->
                studySessionService.submitAnswer(
                    UUID.randomUUID(), card.getId(), ReviewRating.GOOD, 0L))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void endProducesSummaryFromAccumulatedCounters() {
    Deck deck = deckService.create("EndSummary", null, null);
    Card card = cardService.create(deck.getId(), "apple", "사과");

    StudySessionStart start = studySessionService.start(deck.getId());
    UUID sessionId = start.session().getId();
    studySessionService.submitAnswer(sessionId, card.getId(), ReviewRating.GOOD, 4000L);

    SessionSummary summary = studySessionService.end(sessionId);

    assertThat(summary.cardsReviewed()).isEqualTo(1);
    assertThat(summary.correctAnswers()).isEqualTo(1);
    assertThat(summary.accuracy()).isEqualTo(1.0);
    assertThat(summary.totalTimeSpent()).isEqualTo(4.0);
  }

  @Test
  void endIsIdempotent() {
    Deck deck = deckService.create("Idempotent", null, null);
    Card card = cardService.create(deck.getId(), "f", "b");
    StudySessionStart start = studySessionService.start(deck.getId());
    UUID sessionId = start.session().getId();
    studySessionService.submitAnswer(sessionId, card.getId(), ReviewRating.AGAIN, 1000L);

    SessionSummary first = studySessionService.end(sessionId);
    Instant endedAt = studySessionRepository.findById(sessionId).orElseThrow().getEndedAt();
    assertThat(endedAt).isNotNull();

    // Re-ending keeps the original endedAt and returns the same statistics (BR-SES-3).
    SessionSummary second = studySessionService.end(sessionId);
    assertThat(studySessionRepository.findById(sessionId).orElseThrow().getEndedAt())
        .isEqualTo(endedAt);
    assertThat(second.cardsReviewed()).isEqualTo(first.cardsReviewed());
    assertThat(second.correctAnswers()).isEqualTo(first.correctAnswers());
    assertThat(second.accuracy()).isEqualTo(first.accuracy());
    assertThat(second.totalTimeSpent()).isEqualTo(first.totalTimeSpent());
    // AGAIN-only session -> 0 correct, 0 accuracy.
    assertThat(first.correctAnswers()).isZero();
    assertThat(first.accuracy()).isZero();
  }

  @Test
  void endUnknownSessionThrowsNotFound() {
    assertThatThrownBy(() -> studySessionService.end(UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
  }
}
