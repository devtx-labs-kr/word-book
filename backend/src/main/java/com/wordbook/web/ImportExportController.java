package com.wordbook.web;

import com.wordbook.domain.Deck;
import com.wordbook.service.DeckImportExportService;
import com.wordbook.web.dto.DeckResponse;
import com.wordbook.web.dto.ImportPreview;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deck export / import endpoints (U5, FR-7). Errors reuse the existing {@code
 * GlobalExceptionHandler} mappings — {@code InvalidImportFormatException} / {@code
 * HttpMessageNotReadableException} -&gt; 400, {@code NotFoundException} -&gt; 404 — no new handler.
 *
 * <p>Import bodies arrive as raw {@code application/json} bytes ({@code @RequestBody byte[]}); no
 * multipart (frontend-components §4.2).
 */
@RestController
@RequestMapping("/api/decks")
public class ImportExportController {

  private final DeckImportExportService service;

  public ImportExportController(DeckImportExportService service) {
    this.service = service;
  }

  /** Exports a deck as a downloadable JSON attachment (US-E1). */
  @GetMapping("/{id}/export")
  public ResponseEntity<byte[]> export(@PathVariable UUID id) {
    byte[] body = service.exportDeck(id);
    String filename = service.suggestedFilename(id);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename));
    return ResponseEntity.ok().headers(headers).body(body);
  }

  /** Previews an import candidate without persisting (US-E2 step 2). */
  @PostMapping("/import/preview")
  public ImportPreview preview(@RequestBody(required = false) byte[] body) {
    return service.validate(body);
  }

  /** Imports a deck, returning the created deck as 201 (US-E2). */
  @PostMapping("/import")
  public ResponseEntity<DeckResponse> importDeck(@RequestBody(required = false) byte[] body) {
    Deck deck = service.importDeck(body);
    return ResponseEntity.status(HttpStatus.CREATED).body(DeckResponse.from(deck));
  }

  /**
   * Builds a {@code Content-Disposition} attachment header. Always includes an ASCII {@code
   * filename} fallback plus an RFC 5987 {@code filename*=UTF-8''...} for non-ASCII deck names
   * (Korean, etc.) so the download name does not break.
   */
  private static String contentDisposition(String filename) {
    String asciiFallback = filename.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");
    String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;
  }
}
