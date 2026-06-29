package com.wordbook.web.dto;

import com.wordbook.domain.Card;

/**
 * Per-card statistics row for {@code GET /api/decks/{deckId}/card-stats} (FR-6.5, BR-ST6). {@code
 * accuracy} comes from {@link Card#getAccuracy()} (0 when no reviews, BR-ST8). Response-only DTO.
 *
 * @param word the card front (the word being learned)
 * @param interval current SM-2 interval in days
 * @param easeFactor current SM-2 ease factor
 * @param totalReviews total reviews so far
 * @param accuracy {@code correctReviews / totalReviews}, 0 when none
 */
public record CardStat(
    String word, int interval, double easeFactor, int totalReviews, double accuracy) {

  public static CardStat from(Card card) {
    return new CardStat(
        card.getFront(),
        card.getInterval(),
        card.getEaseFactor(),
        card.getTotalReviews(),
        card.getAccuracy());
  }
}
