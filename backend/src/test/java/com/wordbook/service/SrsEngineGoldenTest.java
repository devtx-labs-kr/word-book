package com.wordbook.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wordbook.domain.Card;
import com.wordbook.domain.LearningState;
import com.wordbook.domain.ReviewRating;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Golden tests for {@link SrsEngine}, the exact port of the Swift {@code SRSEngine}. Implements the
 * full table from business-logic-model §3, including the interval=20 (LEARNING) / interval=21
 * (MASTERED) boundary cases, the EF lower bound 1.3, the FP truncation case {@code (int)(3*1.2)=3},
 * and the AGAIN case. These are mandatory (project.md Mandated).
 */
class SrsEngineGoldenTest {

  private static final double EF_DELTA = 1e-9;
  private final SrsEngine engine = new SrsEngine();

  private static Card card(LearningState state, double easeFactor, int interval, int repetitions) {
    Card c = new Card("front", "back");
    c.setLearningState(state);
    c.setEaseFactor(easeFactor);
    c.setInterval(interval);
    c.setRepetitions(repetitions);
    return c;
  }

  @Test
  @DisplayName("new, interval0, rep0, EF2.5 + GOOD -> interval1, EF2.5, NEW (kept)")
  void newCardGoodFirstReview() {
    SrsResult r = engine.calculateNextReview(card(LearningState.NEW, 2.5, 0, 0), ReviewRating.GOOD);
    assertThat(r.interval()).isEqualTo(1);
    assertThat(r.easeFactor()).isCloseTo(2.5, org.assertj.core.data.Offset.offset(EF_DELTA));
    // interval is 1, not > 1, so NEW stays NEW.
    assertThat(r.state()).isEqualTo(LearningState.NEW);
  }

  @Test
  @DisplayName("interval1, rep1, EF2.5 + GOOD -> interval6, LEARNING")
  void secondReviewGood() {
    SrsResult r = engine.calculateNextReview(card(LearningState.NEW, 2.5, 1, 1), ReviewRating.GOOD);
    assertThat(r.interval()).isEqualTo(6);
    assertThat(r.state()).isEqualTo(LearningState.LEARNING);
  }

  @Test
  @DisplayName("interval6, rep2, EF2.5 + GOOD -> (int)(6*2.5)=15, LEARNING")
  void matureGood() {
    SrsResult r = engine.calculateNextReview(card(LearningState.NEW, 2.5, 6, 2), ReviewRating.GOOD);
    assertThat(r.interval()).isEqualTo(15);
    assertThat(r.state()).isEqualTo(LearningState.LEARNING);
  }

  @Test
  @DisplayName("interval15, rep3, EF2.5 + GOOD -> (int)(15*2.5)=37 -> MASTERED")
  void reachesMastered() {
    SrsResult r =
        engine.calculateNextReview(card(LearningState.LEARNING, 2.5, 15, 3), ReviewRating.GOOD);
    assertThat(r.interval()).isEqualTo(37);
    assertThat(r.state()).isEqualTo(LearningState.MASTERED);
  }

  @Test
  @DisplayName("BOUNDARY: interval8, rep2 + GOOD -> (int)(8*2.5)=20 (<21) -> LEARNING")
  void boundaryUnder21() {
    SrsResult r = engine.calculateNextReview(card(LearningState.NEW, 2.5, 8, 2), ReviewRating.GOOD);
    assertThat(r.interval()).isEqualTo(20);
    assertThat(r.state()).isEqualTo(LearningState.LEARNING);
  }

  @Test
  @DisplayName("BOUNDARY: interval9, rep2 + GOOD -> (int)(9*2.5)=22 (>=21) -> MASTERED")
  void boundaryReach21() {
    SrsResult r = engine.calculateNextReview(card(LearningState.NEW, 2.5, 9, 2), ReviewRating.GOOD);
    assertThat(r.interval()).isEqualTo(22);
    assertThat(r.state()).isEqualTo(LearningState.MASTERED);
  }

  @Test
  @DisplayName("BOUNDARY: interval14, EF1.5, rep2 + GOOD -> (int)(14*1.5)=21 (==21) -> MASTERED")
  void boundaryExactly21() {
    // EF' for GOOD from 1.5 stays 1.5 (increment 0.0), so (int)(14*1.5)=21 exactly.
    // Pins the >=21 transition so a regression to >21 would fail here.
    SrsResult r =
        engine.calculateNextReview(card(LearningState.NEW, 1.5, 14, 2), ReviewRating.GOOD);
    assertThat(r.interval()).isEqualTo(21);
    assertThat(r.state()).isEqualTo(LearningState.MASTERED);
  }

  @Test
  @DisplayName("new, rep0 + EASY -> interval4, EF2.6, LEARNING")
  void newCardEasy() {
    SrsResult r = engine.calculateNextReview(card(LearningState.NEW, 2.5, 0, 0), ReviewRating.EASY);
    assertThat(r.interval()).isEqualTo(4);
    assertThat(r.easeFactor()).isCloseTo(2.6, org.assertj.core.data.Offset.offset(EF_DELTA));
    assertThat(r.state()).isEqualTo(LearningState.LEARNING);
  }

  @Test
  @DisplayName("any + AGAIN -> interval1, EF unchanged, RELEARNING")
  void againResets() {
    Card c = card(LearningState.MASTERED, 2.3, 40, 5);
    SrsResult r = engine.calculateNextReview(c, ReviewRating.AGAIN);
    assertThat(r.interval()).isEqualTo(1);
    assertThat(r.easeFactor()).isCloseTo(2.3, org.assertj.core.data.Offset.offset(EF_DELTA));
    assertThat(r.state()).isEqualTo(LearningState.RELEARNING);
  }

  @Test
  @DisplayName("FP TRUNCATION: interval3, rep2 + HARD -> max(1,(int)(3*1.2))=(int)3.599=3")
  void hardTruncation() {
    SrsResult r =
        engine.calculateNextReview(card(LearningState.LEARNING, 2.5, 3, 2), ReviewRating.HARD);
    assertThat(r.interval()).isEqualTo(3);
  }

  @Test
  @DisplayName("EF LOWER BOUND: EF1.3 + HARD -> EF stays 1.3 (not below)")
  void easeFactorLowerBound() {
    SrsResult r =
        engine.calculateNextReview(card(LearningState.LEARNING, 1.3, 5, 2), ReviewRating.HARD);
    assertThat(r.easeFactor()).isCloseTo(1.3, org.assertj.core.data.Offset.offset(EF_DELTA));
  }

  @Test
  @DisplayName("EF increments: HARD=-0.14 / GOOD=0.0 / EASY=+0.1 from EF2.5")
  void easeFactorIncrements() {
    assertThat(
            engine
                .calculateNextReview(card(LearningState.LEARNING, 2.5, 5, 2), ReviewRating.HARD)
                .easeFactor())
        .isCloseTo(2.36, org.assertj.core.data.Offset.offset(EF_DELTA));
    assertThat(
            engine
                .calculateNextReview(card(LearningState.LEARNING, 2.5, 5, 2), ReviewRating.GOOD)
                .easeFactor())
        .isCloseTo(2.5, org.assertj.core.data.Offset.offset(EF_DELTA));
    assertThat(
            engine
                .calculateNextReview(card(LearningState.LEARNING, 2.5, 5, 2), ReviewRating.EASY)
                .easeFactor())
        .isCloseTo(2.6, org.assertj.core.data.Offset.offset(EF_DELTA));
  }

  @Test
  @DisplayName("No demotion: MASTERED with small interval stays MASTERED")
  void noDemotion() {
    // HARD on a mastered card yields a small interval, but state must not drop below MASTERED.
    SrsResult r =
        engine.calculateNextReview(card(LearningState.MASTERED, 2.5, 2, 2), ReviewRating.HARD);
    assertThat(r.interval()).isEqualTo(2); // max(1,(int)(2*1.2))=(int)2.4=2
    assertThat(r.state()).isEqualTo(LearningState.MASTERED);
  }
}
