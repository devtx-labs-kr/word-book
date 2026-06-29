package com.wordbook.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A study session. {@code deckId} is a loose reference (no FK constraint) so session history
 * survives deck deletion (data-rule DR-3); deleted decks show as "Unknown Deck" in statistics.
 */
@Entity
@Table(name = "study_session")
public class StudySession {

  @Id private UUID id = UUID.randomUUID();

  private Instant startedAt = Instant.now();
  private Instant endedAt;

  /** Loose reference — intentionally NOT a JPA relationship (DR-3). */
  private UUID deckId;

  private int cardsReviewed = 0;
  private int correctAnswers = 0;
  private double totalTimeSpent = 0;

  protected StudySession() {}

  public StudySession(UUID deckId) {
    this.deckId = deckId;
  }

  public UUID getId() {
    return id;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public void setEndedAt(Instant endedAt) {
    this.endedAt = endedAt;
  }

  public UUID getDeckId() {
    return deckId;
  }

  public int getCardsReviewed() {
    return cardsReviewed;
  }

  public void setCardsReviewed(int cardsReviewed) {
    this.cardsReviewed = cardsReviewed;
  }

  public int getCorrectAnswers() {
    return correctAnswers;
  }

  public void setCorrectAnswers(int correctAnswers) {
    this.correctAnswers = correctAnswers;
  }

  public double getTotalTimeSpent() {
    return totalTimeSpent;
  }

  public void setTotalTimeSpent(double totalTimeSpent) {
    this.totalTimeSpent = totalTimeSpent;
  }

  /** Accuracy ratio; 0 when no cards reviewed yet. */
  public double getAccuracy() {
    if (cardsReviewed == 0) {
      return 0;
    }
    return (double) correctAnswers / cardsReviewed;
  }

  public boolean isActive() {
    return endedAt == null;
  }
}
