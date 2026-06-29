package com.wordbook.web;

import com.wordbook.domain.Deck;
import com.wordbook.service.DeckService;
import com.wordbook.web.dto.CreateDeckRequest;
import com.wordbook.web.dto.DeckResponse;
import com.wordbook.web.dto.DeckSummaryResponse;
import com.wordbook.web.dto.UpdateDeckRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for decks: list/search/sort + full CRUD (FR-1). */
@RestController
@RequestMapping("/api/decks")
public class DeckController {

  private final DeckService deckService;

  public DeckController(DeckService deckService) {
    this.deckService = deckService;
  }

  @GetMapping
  public List<DeckSummaryResponse> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "name") String sort) {
    return deckService.list(search, sort).stream().map(DeckSummaryResponse::from).toList();
  }

  @GetMapping("/{id}")
  public DeckResponse get(@PathVariable UUID id) {
    return DeckResponse.from(deckService.get(id));
  }

  @PostMapping
  public ResponseEntity<DeckResponse> create(@Valid @RequestBody CreateDeckRequest request) {
    Deck deck = deckService.create(request.getName(), request.getDescription(), request.getColor());
    return ResponseEntity.status(HttpStatus.CREATED).body(DeckResponse.from(deck));
  }

  @PutMapping("/{id}")
  public DeckResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDeckRequest request) {
    Deck deck =
        deckService.update(id, request.getName(), request.getDescription(), request.getColor());
    return DeckResponse.from(deck);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    deckService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
