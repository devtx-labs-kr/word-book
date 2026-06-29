package com.wordbook.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A deck of flashcards. Ported from the Swift {@code Deck} SwiftData model. */
@Entity
@Table(name = "deck")
public class Deck {

  @Id private UUID id = UUID.randomUUID();

  private String name;

  /**
   * Free-text description. The Java field is {@code description}, but the JSON key is {@code
   * descriptionText} for compatibility with the original macOS export format.
   */
  @JsonProperty("descriptionText")
  private String description = "";

  private String color = "#007AFF";

  private Instant createdAt = Instant.now();

  private Instant updatedAt = Instant.now();

  @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Card> cards = new ArrayList<>();

  protected Deck() {}

  public Deck(String name, String description, String color) {
    this.name = name;
    if (description != null) {
      this.description = description;
    }
    if (color != null) {
      this.color = color;
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
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

  public List<Card> getCards() {
    return cards;
  }

  public void addCard(Card card) {
    cards.add(card);
    card.setDeck(this);
  }

  // --- Derived properties (FR-1) ---

  public int getTotalCards() {
    return cards.size();
  }

  public long getDueCards() {
    Instant now = Instant.now();
    return cards.stream().filter(c -> !c.getNextReviewDate().isAfter(now)).count();
  }

  public long getNewCards() {
    return cards.stream().filter(c -> c.getLearningState() == LearningState.NEW).count();
  }

  public long getLearningCards() {
    return cards.stream().filter(c -> c.getLearningState() == LearningState.LEARNING).count();
  }

  public long getMasteredCards() {
    return cards.stream().filter(c -> c.getLearningState() == LearningState.MASTERED).count();
  }
}
