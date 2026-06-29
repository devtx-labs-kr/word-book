package com.wordbook.web;

import com.wordbook.domain.Card;
import com.wordbook.service.CardService;
import com.wordbook.web.dto.CardResponse;
import com.wordbook.web.dto.CreateCardRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for cards nested under a deck: list (filter/search) + create. Single-card
 * update/delete live in {@link CardItemController} under {@code /api/cards} to avoid clashing with
 * this class-level mapping (logical-components ★).
 */
@RestController
@RequestMapping("/api/decks/{deckId}/cards")
public class CardController {

  private final CardService cardService;

  public CardController(CardService cardService) {
    this.cardService = cardService;
  }

  @GetMapping
  public List<CardResponse> list(
      @PathVariable UUID deckId,
      @RequestParam(required = false, defaultValue = "All") String filter,
      @RequestParam(required = false) String search) {
    return cardService.listByDeck(deckId, filter, search).stream().map(CardResponse::from).toList();
  }

  @PostMapping
  public ResponseEntity<CardResponse> create(
      @PathVariable UUID deckId, @Valid @RequestBody CreateCardRequest request) {
    Card card = cardService.create(deckId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(CardResponse.from(card));
  }
}
