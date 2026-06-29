package com.wordbook.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Read-only preview of an import candidate (U5 validate / US-E2 step 2). {@code valid} signals
 * whether the bytes decode into a {@code DeckExportDto} (VR-1); when false the remaining fields are
 * meaningless. {@code duplicateName} carries the final name that import would actually apply
 * ({@code "<name> (Imported)"}) when a deck of the same name already exists, else null (DR-3).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImportPreview(boolean valid, String deckName, int cardCount, String duplicateName) {

  /** Preview for a payload that could not be decoded (VR-1). */
  public static ImportPreview invalid() {
    return new ImportPreview(false, null, 0, null);
  }
}
