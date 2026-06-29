package com.wordbook.repository;

import com.wordbook.domain.Card;
import com.wordbook.domain.LearningState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Card}, including due/new selection queries (BR-1/BR-2, PD-2). */
public interface CardRepository extends JpaRepository<Card, UUID> {

  List<Card> findByDeckId(UUID deckId);

  /** State filter (SR-3, business-logic-model §3.2). */
  List<Card> findByDeckIdAndLearningState(UUID deckId, LearningState learningState);

  /** Due cards: nextReviewDate <= now, ascending (BR-1). DB-side sort + limit (PD-2). */
  List<Card> findByDeckIdAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(
      UUID deckId, Instant now, Pageable pageable);

  /** New cards: state == NEW, ascending by creation time (BR-2). */
  List<Card> findByDeckIdAndLearningStateOrderByCreatedAtAsc(
      UUID deckId, LearningState learningState, Pageable pageable);

  // --- Statistics (U4, FR-6.2/6.4): DB-side counts over the existing indexes (PD-D-U4-2/3). ---

  /** Count of cards in a given learning state — overall distribution (BR-ST3). */
  long countByLearningState(LearningState learningState);

  /** Cumulative due count: cards whose nextReviewDate is before {@code boundary} (BR-ST5). */
  long countByNextReviewDateLessThan(Instant boundary);

  /** Windowed due count over the half-open range {@code [start, end)} (BR-ST5, tomorrow window). */
  long countByNextReviewDateGreaterThanEqualAndNextReviewDateLessThan(Instant start, Instant end);
}
