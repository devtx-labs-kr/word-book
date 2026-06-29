package com.wordbook.web;

import com.wordbook.domain.Card;
import com.wordbook.service.CardService;
import com.wordbook.web.dto.CardResponse;
import com.wordbook.web.dto.UpdateCardRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single-card endpoints addressed by card id ({@code /api/cards/{id}}). Kept separate from {@link
 * CardController} (which is fixed to {@code /api/decks/{deckId}/cards}) so the two base paths do
 * not clash (logical-components ★).
 */
@RestController
@RequestMapping("/api/cards")
public class CardItemController {

  private final CardService cardService;

  public CardItemController(CardService cardService) {
    this.cardService = cardService;
  }

  @PutMapping("/{id}")
  public CardResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCardRequest request) {
    Card card = cardService.update(id, request);
    return CardResponse.from(card);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    cardService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
