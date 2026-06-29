package com.wordbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wordbook.domain.Deck;
import com.wordbook.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Service-level tests for deck list/search/sort + update/delete (FR-1, SR-1/SR-2, DR-1/DR-4). */
@SpringBootTest
class DeckServiceTest {

  @Autowired private DeckService deckService;

  /** Filters list results to those created by this test via a unique name marker. */
  private List<Deck> withMarker(List<Deck> decks, String marker) {
    return decks.stream().filter(d -> d.getName().contains(marker)).toList();
  }

  @Test
  void searchMatchesNameCaseInsensitively() {
    String marker = "SRCH-" + UUID.randomUUID();
    deckService.create("Spanish " + marker, null, null);
    deckService.create("French " + marker, null, null);

    List<Deck> hits = deckService.list("spanish", "name");
    assertThat(withMarker(hits, marker)).hasSize(1);
    assertThat(hits.get(0).getName()).contains("Spanish");
  }

  @Test
  void searchWithNoMatchReturnsEmpty() {
    List<Deck> hits = deckService.list("no-such-deck-" + UUID.randomUUID(), "name");
    assertThat(hits).isEmpty();
  }

  @Test
  void sortByNameAscending() {
    String marker = "SORT-" + UUID.randomUUID();
    deckService.create("Zeta " + marker, null, null);
    deckService.create("Alpha " + marker, null, null);

    List<Deck> sorted = withMarker(deckService.list(marker, "name"), marker);
    assertThat(sorted).hasSize(2);
    assertThat(sorted.get(0).getName()).contains("Alpha");
    assertThat(sorted.get(1).getName()).contains("Zeta");
  }

  @Test
  void unrecognizedSortFallsBackToName() {
    String marker = "FALLBACK-" + UUID.randomUUID();
    deckService.create("Yankee " + marker, null, null);
    deckService.create("Bravo " + marker, null, null);

    List<Deck> sorted = withMarker(deckService.list(marker, "bogus"), marker);
    assertThat(sorted.get(0).getName()).contains("Bravo");
    assertThat(sorted.get(1).getName()).contains("Yankee");
  }

  @Test
  void updateChangesFieldsAndBumpsUpdatedAt() {
    Deck deck = deckService.create("Original", "desc", "#007AFF");
    var before = deck.getUpdatedAt();

    Deck updated = deckService.update(deck.getId(), "Renamed", "new desc", "#FF3B30");
    assertThat(updated.getName()).isEqualTo("Renamed");
    assertThat(updated.getDescription()).isEqualTo("new desc");
    assertThat(updated.getColor()).isEqualTo("#FF3B30");
    assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(before);
  }

  @Test
  void updateUnknownDeckThrowsNotFound() {
    assertThatThrownBy(() -> deckService.update(UUID.randomUUID(), "x", null, null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void deleteUnknownDeckThrowsNotFound() {
    assertThatThrownBy(() -> deckService.delete(UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
  }
}
