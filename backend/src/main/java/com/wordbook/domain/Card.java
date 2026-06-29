package com.wordbook.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single flashcard with SM-2 spaced-repetition state. Ported from the Swift {@code Card}
 * SwiftData model.
 *
 * <p>Indexes on {@code nextReviewDate} and {@code learningState} accelerate due/statistics queries
 * (performance-design PD-1).
 */
@Entity
@Table(
    name = "card",
    indexes = {
      @Index(name = "idx_card_next_review_date", columnList = "nextReviewDate"),
      @Index(name = "idx_card_learning_state", columnList = "learningState")
    })
public class Card {

  @Id private UUID id = UUID.randomUUID();

  private String front;
  private String back;
  private String example;
  private String pronunciation;
  private String notes;

  // --- SRS data ---
  @Enumerated(EnumType.STRING)
  private LearningState learningState = LearningState.NEW;

  private double easeFactor = 2.5;

  // "interval" is a reserved SQL keyword; map to a safe column name.
  @Column(name = "review_interval")
  private int interval = 0;

  private int repetitions = 0;
  private Instant nextReviewDate = Instant.now();
  private Instant lastReviewedAt;

  // --- Statistics ---
  private int totalReviews = 0;
  private int correctReviews = 0;
  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();

  @ManyToOne
  @JoinColumn(name = "deck_id")
  private Deck deck;

  @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ReviewRecord> reviewRecords = new ArrayList<>();

  protected Card() {}

  public Card(String front, String back) {
    this.front = front;
    this.back = back;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getFront() {
    return front;
  }

  public void setFront(String front) {
    this.front = front;
  }

  public String getBack() {
    return back;
  }

  public void setBack(String back) {
    this.back = back;
  }

  public String getExample() {
    return example;
  }

  public void setExample(String example) {
    this.example = example;
  }

  public String getPronunciation() {
    return pronunciation;
  }

  public void setPronunciation(String pronunciation) {
    this.pronunciation = pronunciation;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public LearningState getLearningState() {
    return learningState;
  }

  public void setLearningState(LearningState learningState) {
    this.learningState = learningState;
  }

  public double getEaseFactor() {
    return easeFactor;
  }

  public void setEaseFactor(double easeFactor) {
    this.easeFactor = easeFactor;
  }

  public int getInterval() {
    return interval;
  }

  public void setInterval(int interval) {
    this.interval = interval;
  }

  public int getRepetitions() {
    return repetitions;
  }

  public void setRepetitions(int repetitions) {
    this.repetitions = repetitions;
  }

  public Instant getNextReviewDate() {
    return nextReviewDate;
  }

  public void setNextReviewDate(Instant nextReviewDate) {
    this.nextReviewDate = nextReviewDate;
  }

  public Instant getLastReviewedAt() {
    return lastReviewedAt;
  }

  public void setLastReviewedAt(Instant lastReviewedAt) {
    this.lastReviewedAt = lastReviewedAt;
  }

  public int getTotalReviews() {
    return totalReviews;
  }

  public void setTotalReviews(int totalReviews) {
    this.totalReviews = totalReviews;
  }

  public int getCorrectReviews() {
    return correctReviews;
  }

  public void setCorrectReviews(int correctReviews) {
    this.correctReviews = correctReviews;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Deck getDeck() {
    return deck;
  }

  public void setDeck(Deck deck) {
    this.deck = deck;
  }

  public List<ReviewRecord> getReviewRecords() {
    return reviewRecords;
  }

  /** Accuracy ratio; 0 when there are no reviews yet. */
  public double getAccuracy() {
    if (totalReviews == 0) {
      return 0;
    }
    return (double) correctReviews / totalReviews;
  }
}
