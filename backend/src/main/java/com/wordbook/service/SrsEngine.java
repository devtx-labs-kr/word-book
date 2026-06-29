package com.wordbook.service;

import com.wordbook.domain.Card;
import com.wordbook.domain.Deck;
import com.wordbook.domain.LearningState;
import com.wordbook.domain.ReviewRating;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * SM-2 spaced-repetition engine. Exact 1:1 port of the original Swift {@code SRSEngine} (codekb
 * architecture §2). Pure, deterministic, stateless — the subject of the golden tests.
 *
 * <p>Source of the formula: SuperMemo SM-2. The integer interval conversion uses a Java {@code
 * (int)} cast (truncation toward zero) to match the Swift {@code Int(Double)} behaviour exactly —
 * {@code Math.round}/{@code Math.floor} are forbidden (project.md Mandated, IV-1).
 */
@Component
public class SrsEngine {

  /**
   * Computes the next review schedule for a card given a rating. Returns {@code (interval,
   * easeFactor, state)}; the caller updates {@code repetitions} (AGAIN -> 0, else +1).
   */
  public SrsResult calculateNextReview(Card card, ReviewRating rating) {
    int newInterval;
    double newEaseFactor = card.getEaseFactor();
    LearningState newState = card.getLearningState();
    int repetitions = card.getRepetitions();

    // SM-2 ease factor adjustment (only when not AGAIN; AGAIN leaves EF unchanged — IV-2).
    if (rating != ReviewRating.AGAIN) {
      // quality mapped to 2-5 (AGAIN=2 unused, HARD=3, GOOD=4, EASY=5).
      double qualityScore = rating.getValue() + 2;
      // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)); lower bound 1.3.
      newEaseFactor =
          Math.max(
              1.3,
              card.getEaseFactor()
                  + (0.1 - (5 - qualityScore) * (0.08 + (5 - qualityScore) * 0.02)));
    }

    switch (rating) {
      case AGAIN -> {
        // Wrong: restart. interval=1, repetitions reset by caller, RELEARNING.
        newInterval = 1;
        repetitions = 0;
        newState = LearningState.RELEARNING;
      }
      case HARD -> {
        if (repetitions == 0) {
          newInterval = 1;
        } else if (repetitions == 1) {
          newInterval = 3;
        } else {
          // Fixed 1.2x, no EF, lower bound max(1, ...). (int) truncates toward zero.
          newInterval = Math.max(1, (int) (card.getInterval() * 1.2));
        }
      }
      case GOOD -> {
        if (repetitions == 0) {
          newInterval = 1;
        } else if (repetitions == 1) {
          newInterval = 6;
        } else {
          newInterval = (int) (card.getInterval() * newEaseFactor);
        }
      }
      case EASY -> {
        if (repetitions == 0) {
          newInterval = 4;
        } else if (repetitions == 1) {
          newInterval = 10;
        } else {
          newInterval = (int) (card.getInterval() * newEaseFactor * 1.3);
        }
      }
      default -> throw new IllegalArgumentException("Unknown rating: " + rating);
    }

    // State transition (after interval is known). interval>=21 -> MASTERED takes top priority;
    // else interval>1 && currentState==NEW -> LEARNING. No demotion (IV-3/IV-4).
    if (newInterval >= 21) {
      newState = LearningState.MASTERED;
    } else if (newInterval > 1 && newState == LearningState.NEW) {
      newState = LearningState.LEARNING;
    }

    return new SrsResult(newInterval, newEaseFactor, newState);
  }

  /** Due cards (in-memory variant): nextReviewDate <= now, ascending, optionally limited. */
  public List<Card> getDueCards(Deck deck, Integer limit) {
    Instant now = Instant.now();
    var stream =
        deck.getCards().stream()
            .filter(c -> !c.getNextReviewDate().isAfter(now))
            .sorted(Comparator.comparing(Card::getNextReviewDate));
    if (limit != null) {
      stream = stream.limit(limit);
    }
    return stream.toList();
  }

  /** New cards (in-memory variant): state == NEW, ascending by creation time, limited. */
  public List<Card> getNewCards(Deck deck, int limit) {
    return deck.getCards().stream()
        .filter(c -> c.getLearningState() == LearningState.NEW)
        .sorted(Comparator.comparing(Card::getCreatedAt))
        .limit(limit)
        .toList();
  }

  /**
   * Next review date = {@code from} + {@code interval} calendar days in the server-local time zone
   * (matches Swift {@code Calendar.date(byAdding:.day)}, ADR-8). Calendar-day addition, not a flat
   * 24h * interval, so DST shifts land on the same wall-clock time.
   */
  public Instant calculateNextReviewDate(int interval, Instant from) {
    ZoneId zone = ZoneId.systemDefault();
    return from.atZone(zone).plusDays(interval).toInstant();
  }
}
