package com.wordbook.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Top-level serialization envelope for a deck export (U5). Mirrors the original macOS {@code
 * DeckExportDTO} (Swift {@code Codable}) 1:1 — {@code version} is always {@code "1.0"} on export
 * and {@code exportedAt} is the moment of export (CR-6).
 *
 * <p>Serialized/deserialized exclusively by the dedicated {@code ObjectMapper} in {@code
 * DeckImportExportService} — never the global Spring bean.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeckExportDto(String version, Instant exportedAt, DeckDto deck) {}
