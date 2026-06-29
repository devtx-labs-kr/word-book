package com.wordbook.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Serialization view of a {@code Deck} for export/import (U5). The JSON key for the description is
 * {@code descriptionText} (CR-1), matching the original macOS format and the {@code Deck} entity's
 * {@code @JsonProperty}.
 *
 * <p>On import {@code id}/{@code createdAt}/{@code updatedAt} are ignored — the entity regenerates
 * them (FP-1). {@code cards} may be an empty list (VR-4).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeckDto(
    UUID id,
    String name,
    @JsonProperty("descriptionText") String descriptionText,
    String color,
    Instant createdAt,
    Instant updatedAt,
    List<CardDto> cards) {}
