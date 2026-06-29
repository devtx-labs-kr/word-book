package com.wordbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wordbook.domain.Card;
import com.wordbook.domain.Deck;
import com.wordbook.domain.LearningState;
import com.wordbook.exception.InvalidImportFormatException;
import com.wordbook.exception.NotFoundException;
import com.wordbook.repository.DeckRepository;
import com.wordbook.web.dto.ImportPreview;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * U5 import/export service tests. The round-trip cases are mandatory (team.md) — they pin SRS/stat
 * preservation, ISO-8601 second-precision serialization, lenient parsing, nullable key handling,
 * duplicate-name suffixing, the NEW fallback, a real macOS sample, and import atomicity.
 */
@SpringBootTest
class DeckImportExportServiceTest {

  @Autowired private DeckImportExportService service;
  @Autowired private DeckRepository deckRepository;

  private static final Instant NEXT_REVIEW = Instant.parse("2025-01-25T22:05:00Z");
  private static final Instant LAST_REVIEWED = Instant.parse("2025-01-10T22:05:00Z");
  private static final Instant ORIGINAL_CREATED = Instant.parse("2024-11-02T09:15:00Z");

  /** Persists a deck with one fully-populated, SRS-advanced card and returns it. */
  @Transactional
  Deck persistSampleDeck(String name) {
    Deck deck = new Deck(name, "desc", "#34C759");
    deck.setCreatedAt(ORIGINAL_CREATED);
    deck.setUpdatedAt(ORIGINAL_CREATED);
    Card card = new Card("apple", "사과");
    card.setExample("I ate an apple.");
    card.setPronunciation("/ˈæp.əl/");
    card.setNotes("fruit");
    card.setLearningState(LearningState.MASTERED);
    card.setEaseFactor(2.6);
    card.setInterval(15);
    card.setRepetitions(5);
    card.setNextReviewDate(NEXT_REVIEW);
    card.setLastReviewedAt(LAST_REVIEWED);
    card.setTotalReviews(10);
    card.setCorrectReviews(8);
    card.setCreatedAt(ORIGINAL_CREATED);
    card.setUpdatedAt(ORIGINAL_CREATED);
    deck.addCard(card);
    return deckRepository.save(deck);
  }

  @Test
  void roundTripPreservesSrsAndStatsAndRegeneratesIdentity() {
    Deck original = persistSampleDeck("RoundTrip-" + UUID.randomUUID());
    UUID originalDeckId = original.getId();
    UUID originalCardId = original.getCards().get(0).getId();

    byte[] bytes = service.exportDeck(originalDeckId);
    Deck imported = service.importDeck(bytes);

    // Identity regenerated (FP-1).
    assertThat(imported.getId()).isNotEqualTo(originalDeckId);
    Card importedCard = imported.getCards().get(0);
    assertThat(importedCard.getId()).isNotEqualTo(originalCardId);
    assertThat(importedCard.getCreatedAt()).isNotEqualTo(ORIGINAL_CREATED);
    assertThat(imported.getCreatedAt()).isNotEqualTo(ORIGINAL_CREATED);

    // SRS + statistics preserved verbatim — no SM-2 recalculation (FP-2).
    assertThat(importedCard.getLearningState()).isEqualTo(LearningState.MASTERED);
    assertThat(importedCard.getEaseFactor()).isEqualTo(2.6);
    assertThat(importedCard.getInterval()).isEqualTo(15);
    assertThat(importedCard.getRepetitions()).isEqualTo(5);
    assertThat(importedCard.getNextReviewDate()).isEqualTo(NEXT_REVIEW);
    assertThat(importedCard.getLastReviewedAt()).isEqualTo(LAST_REVIEWED);
    assertThat(importedCard.getTotalReviews()).isEqualTo(10);
    assertThat(importedCard.getCorrectReviews()).isEqualTo(8);

    // Content copied.
    assertThat(importedCard.getFront()).isEqualTo("apple");
    assertThat(importedCard.getBack()).isEqualTo("사과");
    assertThat(importedCard.getExample()).isEqualTo("I ate an apple.");
  }

  @Test
  void exportEmitsIso8601SecondsWithoutFractionalSeconds() {
    Deck deck = persistSampleDeck("IsoFormat-" + UUID.randomUUID());
    String json = new String(service.exportDeck(deck.getId()), StandardCharsets.UTF_8);

    // Exact second-precision UTC value present.
    assertThat(json).contains("2025-01-25T22:05:00Z");
    // No fractional seconds anywhere (the #1 macOS round-trip risk).
    assertThat(json).doesNotContainPattern("\\d\\d:\\d\\d\\.\\d");
    // No timezone offset form on export (always trailing Z).
    assertThat(json).doesNotContainPattern("\\d\\d:\\d\\d[+-]\\d\\d:\\d\\d");
  }

  @Test
  void importAcceptsFractionalSecondsAndOffsetDates() {
    String name = "Lenient-" + UUID.randomUUID();
    String json =
        "{\"version\":\"1.0\",\"exportedAt\":\"2025-01-11T08:00:00.123Z\",\"deck\":{"
            + "\"id\":\"00000000-0000-0000-0000-0000000000bb\",\"name\":\""
            + name
            + "\",\"descriptionText\":\"\",\"color\":\"#007AFF\","
            + "\"createdAt\":\"2024-11-02T09:15:00.123Z\",\"updatedAt\":\"2024-11-02T09:15:00+00:00\","
            + "\"cards\":[{\"front\":\"a\",\"back\":\"b\",\"learningState\":\"new\","
            + "\"easeFactor\":2.5,\"interval\":0,\"repetitions\":0,"
            + "\"nextReviewDate\":\"2025-01-25T22:05:00.500Z\",\"totalReviews\":0,"
            + "\"correctReviews\":0,\"createdAt\":\"2024-11-02T09:15:00Z\","
            + "\"updatedAt\":\"2024-11-02T09:15:00Z\"}]}}";

    Deck imported = service.importDeck(json.getBytes(StandardCharsets.UTF_8));
    assertThat(imported.getCards()).hasSize(1);
    assertThat(imported.getCards().get(0).getNextReviewDate())
        .isEqualTo(Instant.parse("2025-01-25T22:05:00.500Z"));
  }

  @Test
  void exportOmitsNullNullableKeys() {
    Deck deck = new Deck("Nullable-" + UUID.randomUUID(), "", "#007AFF");
    Card card = new Card("front", "back"); // example/pronunciation/notes/lastReviewedAt all null
    deck.addCard(card);
    Deck saved = deckRepository.save(deck);

    String json = new String(service.exportDeck(saved.getId()), StandardCharsets.UTF_8);
    assertThat(json).doesNotContain("\"example\"");
    assertThat(json).doesNotContain("\"pronunciation\"");
    assertThat(json).doesNotContain("\"notes\"");
    assertThat(json).doesNotContain("\"lastReviewedAt\"");
  }

  @Test
  void importAcceptsMissingNullableKeys() {
    String name = "MissingNullable-" + UUID.randomUUID();
    String json =
        "{\"version\":\"1.0\",\"exportedAt\":\"2025-01-11T08:00:00Z\",\"deck\":{"
            + "\"name\":\""
            + name
            + "\",\"descriptionText\":\"\",\"color\":\"#007AFF\","
            + "\"createdAt\":\"2024-11-02T09:15:00Z\",\"updatedAt\":\"2024-11-02T09:15:00Z\","
            + "\"cards\":[{\"front\":\"a\",\"back\":\"b\",\"learningState\":\"new\","
            + "\"easeFactor\":2.5,\"interval\":0,\"repetitions\":0,"
            + "\"nextReviewDate\":\"2025-01-25T22:05:00Z\",\"totalReviews\":0,\"correctReviews\":0,"
            + "\"createdAt\":\"2024-11-02T09:15:00Z\",\"updatedAt\":\"2024-11-02T09:15:00Z\"}]}}";

    Deck imported = service.importDeck(json.getBytes(StandardCharsets.UTF_8));
    Card card = imported.getCards().get(0);
    assertThat(card.getExample()).isNull();
    assertThat(card.getPronunciation()).isNull();
    assertThat(card.getNotes()).isNull();
    assertThat(card.getLastReviewedAt()).isNull();
  }

  @Test
  void duplicateNameGetsImportedSuffix() {
    String name = "Dup-" + UUID.randomUUID();
    Deck original = persistSampleDeck(name);
    byte[] bytes = service.exportDeck(original.getId());

    Deck imported = service.importDeck(bytes);
    assertThat(imported.getName()).isEqualTo(name + " (Imported)");
  }

  @Test
  void unrecognizedLearningStateFallsBackToNew() {
    String name = "Fallback-" + UUID.randomUUID();
    String json =
        "{\"version\":\"1.0\",\"exportedAt\":\"2025-01-11T08:00:00Z\",\"deck\":{"
            + "\"name\":\""
            + name
            + "\",\"descriptionText\":\"\",\"color\":\"#007AFF\","
            + "\"createdAt\":\"2024-11-02T09:15:00Z\",\"updatedAt\":\"2024-11-02T09:15:00Z\","
            + "\"cards\":[{\"front\":\"a\",\"back\":\"b\",\"learningState\":\"frobnicate\","
            + "\"easeFactor\":2.5,\"interval\":0,\"repetitions\":0,"
            + "\"nextReviewDate\":\"2025-01-25T22:05:00Z\",\"totalReviews\":0,\"correctReviews\":0,"
            + "\"createdAt\":\"2024-11-02T09:15:00Z\",\"updatedAt\":\"2024-11-02T09:15:00Z\"}]}}";

    Deck imported = service.importDeck(json.getBytes(StandardCharsets.UTF_8));
    assertThat(imported.getCards().get(0).getLearningState()).isEqualTo(LearningState.NEW);
  }

  @Test
  void importsRealMacosSampleJson() throws Exception {
    byte[] bytes = new ClassPathResource("macos-sample-deck.json").getContentAsByteArray();

    ImportPreview preview = service.validate(bytes);
    assertThat(preview.valid()).isTrue();
    assertThat(preview.deckName()).isEqualTo("기초 영어");
    assertThat(preview.cardCount()).isEqualTo(2);

    Deck imported = service.importDeck(bytes);
    assertThat(imported.getName()).isEqualTo("기초 영어");
    assertThat(imported.getCards()).hasSize(2);

    Card mastered =
        imported.getCards().stream()
            .filter(c -> c.getFront().equals("apple"))
            .findFirst()
            .orElseThrow();
    assertThat(mastered.getLearningState()).isEqualTo(LearningState.MASTERED);
    assertThat(mastered.getEaseFactor()).isEqualTo(2.6);
    assertThat(mastered.getInterval()).isEqualTo(15);

    Card fresh =
        imported.getCards().stream()
            .filter(c -> c.getFront().equals("banana"))
            .findFirst()
            .orElseThrow();
    assertThat(fresh.getLearningState()).isEqualTo(LearningState.NEW);
    assertThat(fresh.getExample()).isNull(); // nullable key omitted in the sample
  }

  @Test
  void importIsAtomicOnBadCardMidList() {
    String name = "Atomic-" + UUID.randomUUID();
    String tooLong = "x".repeat(300); // exceeds the default VARCHAR(255) column -> persist failure
    String json =
        "{\"version\":\"1.0\",\"exportedAt\":\"2025-01-11T08:00:00Z\",\"deck\":{"
            + "\"name\":\""
            + name
            + "\",\"descriptionText\":\"\",\"color\":\"#007AFF\","
            + "\"createdAt\":\"2024-11-02T09:15:00Z\",\"updatedAt\":\"2024-11-02T09:15:00Z\","
            + "\"cards\":["
            + "{\"front\":\"ok\",\"back\":\"b\",\"learningState\":\"new\",\"easeFactor\":2.5,"
            + "\"interval\":0,\"repetitions\":0,\"nextReviewDate\":\"2025-01-25T22:05:00Z\","
            + "\"totalReviews\":0,\"correctReviews\":0,\"createdAt\":\"2024-11-02T09:15:00Z\","
            + "\"updatedAt\":\"2024-11-02T09:15:00Z\"},"
            + "{\"front\":\""
            + tooLong
            + "\",\"back\":\"b\",\"learningState\":\"new\",\"easeFactor\":2.5,\"interval\":0,"
            + "\"repetitions\":0,\"nextReviewDate\":\"2025-01-25T22:05:00Z\",\"totalReviews\":0,"
            + "\"correctReviews\":0,\"createdAt\":\"2024-11-02T09:15:00Z\","
            + "\"updatedAt\":\"2024-11-02T09:15:00Z\"}]}}";

    assertThatThrownBy(() -> service.importDeck(json.getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(Exception.class);

    // Full rollback — no partially-imported deck remains (ER-5 / RD-U5-1).
    assertThat(deckRepository.existsByName(name)).isFalse();
  }

  @Test
  void invalidJsonThrowsInvalidImportFormat() {
    assertThatThrownBy(() -> service.importDeck("not json".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(InvalidImportFormatException.class);
  }

  @Test
  void emptyPayloadThrowsInvalidImportFormat() {
    assertThatThrownBy(() -> service.importDeck(new byte[0]))
        .isInstanceOf(InvalidImportFormatException.class);
  }

  @Test
  void validateReturnsInvalidForGarbage() {
    ImportPreview preview = service.validate("garbage".getBytes(StandardCharsets.UTF_8));
    assertThat(preview.valid()).isFalse();
    assertThat(preview.cardCount()).isZero();
  }

  @Test
  void exportUnknownDeckThrowsNotFound() {
    assertThatThrownBy(() -> service.exportDeck(UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void suggestedFilenameSanitizesAndDatesName() {
    Deck deck = deckRepository.save(new Deck("My/Deck:Name", "", "#007AFF"));
    String filename = service.suggestedFilename(deck.getId());
    assertThat(filename).matches("My-Deck-Name_\\d{4}-\\d{2}-\\d{2}\\.json");
  }
}
