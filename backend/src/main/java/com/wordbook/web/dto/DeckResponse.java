package com.wordbook.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordbook.domain.Deck;
import java.time.Instant;
import java.util.UUID;

/**
 * Summary view of a deck. {@code description} serializes under the JSON key {@code
 * descriptionText}.
 */
public record DeckResponse(
    UUID id,
    String name,
    @JsonProperty("descriptionText") String description,
    String color,
    Instant createdAt,
    Instant updatedAt,
    int totalCards) {

  public static DeckResponse from(Deck deck) {
    return new DeckResponse(
        deck.getId(),
        deck.getName(),
        deck.getDescription(),
        deck.getColor(),
        deck.getCreatedAt(),
        deck.getUpdatedAt(),
        deck.getTotalCards());
  }
}
