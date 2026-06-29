package com.wordbook.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** An immutable record of a single card review. Ported from the Swift {@code ReviewRecord}. */
@Entity
@Table(name = "review_record")
public class ReviewRecord {

  @Id private UUID id = UUID.randomUUID();

  private Instant reviewedAt = Instant.now();

  @Enumerated(EnumType.STRING)
  private ReviewRating rating;

  private int previousInterval;
  private int newInterval;
  private double previousEaseFactor;
  private double newEaseFactor;

  /** Time spent on this review, in seconds. */
  private double timeSpent;

  @ManyToOne
  @JoinColumn(name = "card_id")
  private Card card;

  protected ReviewRecord() {}

  public ReviewRecord(
      Card card,
      ReviewRating rating,
      int previousInterval,
      int newInterval,
      double previousEaseFactor,
      double newEaseFactor,
      double timeSpent) {
    this.card = card;
    this.rating = rating;
    this.previousInterval = previousInterval;
    this.newInterval = newInterval;
    this.previousEaseFactor = previousEaseFactor;
    this.newEaseFactor = newEaseFactor;
    this.timeSpent = timeSpent;
  }

  public UUID getId() {
    return id;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public ReviewRating getRating() {
    return rating;
  }

  public int getPreviousInterval() {
    return previousInterval;
  }

  public int getNewInterval() {
    return newInterval;
  }

  public double getPreviousEaseFactor() {
    return previousEaseFactor;
  }

  public double getNewEaseFactor() {
    return newEaseFactor;
  }

  public double getTimeSpent() {
    return timeSpent;
  }

  public Card getCard() {
    return card;
  }

  public void setCard(Card card) {
    this.card = card;
  }
}
