package com.wordbook.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordbook.domain.Deck;
import java.time.Instant;
import java.util.UUID;

/**
 * Deck list-row view: the {@link DeckResponse} fields plus the derived {@code dueCards}/{@code
 * newCards} statistics used by the deck list badges (component-methods {@code DeckSummary}). The
 * description serializes under the JSON key {@code descriptionText}.
 */
public record DeckSummaryResponse(
    UUID id,
    String name,
    @JsonProperty("descriptionText") String description,
    String color,
    Instant createdAt,
    Instant updatedAt,
    int totalCards,
    long dueCards,
    long newCards) {

  public static DeckSummaryResponse from(Deck deck) {
    return new DeckSummaryResponse(
        deck.getId(),
        deck.getName(),
        deck.getDescription(),
        deck.getColor(),
        deck.getCreatedAt(),
        deck.getUpdatedAt(),
        deck.getTotalCards(),
        deck.getDueCards(),
        deck.getNewCards());
  }
}
