package com.wordbook.web.dto;

import com.wordbook.domain.Card;
import com.wordbook.domain.LearningState;
import java.time.Instant;
import java.util.UUID;

/** View of a card including its SM-2 state. {@code learningState} serializes lowercase. */
public record CardResponse(
    UUID id,
    String front,
    String back,
    String example,
    String pronunciation,
    String notes,
    LearningState learningState,
    double easeFactor,
    int interval,
    int repetitions,
    Instant nextReviewDate,
    Instant lastReviewedAt,
    int totalReviews,
    int correctReviews) {

  public static CardResponse from(Card card) {
    return new CardResponse(
        card.getId(),
        card.getFront(),
        card.getBack(),
        card.getExample(),
        card.getPronunciation(),
        card.getNotes(),
        card.getLearningState(),
        card.getEaseFactor(),
        card.getInterval(),
        card.getRepetitions(),
        card.getNextReviewDate(),
        card.getLastReviewedAt(),
        card.getTotalReviews(),
        card.getCorrectReviews());
  }
}
