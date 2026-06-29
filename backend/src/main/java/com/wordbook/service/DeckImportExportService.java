package com.wordbook.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wordbook.domain.Card;
import com.wordbook.domain.Deck;
import com.wordbook.domain.LearningState;
import com.wordbook.exception.InvalidImportFormatException;
import com.wordbook.exception.NotFoundException;
import com.wordbook.repository.DeckRepository;
import com.wordbook.web.dto.CardDto;
import com.wordbook.web.dto.DeckDto;
import com.wordbook.web.dto.DeckExportDto;
import com.wordbook.web.dto.ImportPreview;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * macOS-compatible JSON export / validate / import for a single deck (U5, FR-7). Reuses the U1
 * {@code Deck}/{@code Card} entities and repositories; SM-2 ({@code SrsEngine}) is never invoked —
 * import <em>preserves</em> SRS state, it does not recompute it.
 *
 * <p><strong>Dedicated {@code ObjectMapper}.</strong> Serialization is handled by a private mapper
 * built in the constructor — NOT the injected global Spring bean. Reconfiguring the global mapper
 * to second-precision dates + lenient parsing would change date formatting and loosen
 * deserialization across every U1–U4 response, so the blast radius is isolated to this one service
 * (security-design SD-U5-1, business-logic-model §1/§5).
 *
 * <p>The mapper emits ISO-8601 UTC dates truncated to whole seconds ({@code
 * yyyy-MM-dd'T'HH:mm:ss'Z'}) — Jackson's default nanosecond fraction is rejected by Swift's {@code
 * .iso8601} decoder (CR-5). On the read side the lenient {@code JavaTimeModule} {@code Instant}
 * deserializer is kept, so fractional seconds and explicit offsets ({@code ...Z} / {@code
 * ...:00.123Z} / {@code ...+00:00}) are all accepted (FB-3). Polymorphic / default typing is left
 * disabled so the type whitelist stays closed to the fixed DTO graph (no deserialization gadgets,
 * ER-4 / SD-U5-2).
 */
@Service
public class DeckImportExportService {

  /** Date-time format used by the original macOS app: ISO-8601, UTC, no fractional seconds. */
  private static final DateTimeFormatter ISO_SECONDS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

  private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final DeckRepository deckRepository;
  private final ObjectMapper mapper;

  public DeckImportExportService(DeckRepository deckRepository) {
    this.deckRepository = deckRepository;
    this.mapper = buildMapper();
  }

  /**
   * Builds the dedicated export/import mapper. Custom {@code Instant} serializer forces
   * whole-second UTC output; the {@code JavaTimeModule} default deserializer (lenient) is retained
   * for import.
   */
  private static ObjectMapper buildMapper() {
    SimpleModule secondsModule = new SimpleModule();
    secondsModule.addSerializer(
        Instant.class,
        new JsonSerializer<Instant>() {
          @Override
          public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers)
              throws IOException {
            gen.writeString(ISO_SECONDS.format(value));
          }
        });

    return new ObjectMapper()
        // Lenient Instant deserialization (fractional seconds + offset) for import.
        .registerModule(new JavaTimeModule())
        // Override only the Instant serializer to whole-second UTC for export.
        .registerModule(secondsModule)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        // Omit null optionals on export (matches Swift's dropped nil keys, CR-3).
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        // Forward-compatible: ignore unknown keys (SD-U5-4). Does NOT enable polymorphic typing.
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        // Cosmetic: alphabetical keys for byte-closeness to the macOS `sortedKeys` output.
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
  }

  // --- Export (FR-7.1 / US-E1) -------------------------------------------------

  /** Serializes a deck to macOS-compatible export JSON. Unknown deck id -&gt; 404 (ER-2). */
  @Transactional(readOnly = true)
  public byte[] exportDeck(UUID deckId) {
    Deck deck = findDeck(deckId);
    deck.getCards().size(); // initialize the lazy collection while the session is open
    DeckExportDto dto = toExportDto(deck);
    try {
      return mapper.writeValueAsBytes(dto);
    } catch (IOException e) {
      // Surface serialization failure — never swallowed (ER-3).
      throw new IllegalStateException("Failed to serialize deck export: " + deckId, e);
    }
  }

  /**
   * Suggested download filename {@code <sanitized>_<yyyy-MM-dd>.json}. Sanitizes {@code /} and
   * {@code :} to {@code -} and trims (Swift {@code suggestedFilename} 1:1); an empty result falls
   * back to {@code deck} (FB-2). Unknown deck id -&gt; 404.
   */
  @Transactional(readOnly = true)
  public String suggestedFilename(UUID deckId) {
    return buildFilename(findDeck(deckId).getName());
  }

  static String buildFilename(String deckName) {
    String sanitized = deckName == null ? "" : deckName.replace("/", "-").replace(":", "-").strip();
    if (sanitized.isEmpty()) {
      sanitized = "deck";
    }
    return sanitized + "_" + FILE_DATE.format(LocalDate.now()) + ".json";
  }

  // --- Validate / preview (US-E2 step 2) --------------------------------------

  /**
   * Read-only preview: reports decode-ability, deck name, card count, and the duplicate-resolved
   * final name. Never persists.
   */
  @Transactional(readOnly = true)
  public ImportPreview validate(byte[] jsonBytes) {
    DeckExportDto dto;
    try {
      dto = readExport(jsonBytes);
    } catch (InvalidImportFormatException e) {
      return ImportPreview.invalid();
    }
    DeckDto deck = dto.deck();
    int cardCount = deck.cards() == null ? 0 : deck.cards().size();
    String duplicateName =
        deckRepository.existsByName(deck.name()) ? deck.name() + " (Imported)" : null;
    return new ImportPreview(true, deck.name(), cardCount, duplicateName);
  }

  // --- Import (FR-7.2 / FR-7.3 / US-E2) ---------------------------------------

  /**
   * Imports a deck atomically (single transaction, all-or-nothing — ER-5 / RD-U5-1). Always creates
   * a <em>new</em> deck (never overwrites); a duplicate name is suffixed {@code " (Imported)"}
   * (DR-1). Card id/createdAt/updatedAt are regenerated (FP-1); SRS + statistics are preserved with
   * no SM-2 recalculation (FP-2). Malformed input -&gt; {@link InvalidImportFormatException} (400).
   */
  @Transactional
  public Deck importDeck(byte[] jsonBytes) {
    DeckExportDto dto = readExport(jsonBytes);
    DeckDto deckDto = dto.deck();

    String name = deckDto.name();
    String finalName = deckRepository.existsByName(name) ? name + " (Imported)" : name;

    Deck deck = new Deck(finalName, deckDto.descriptionText(), deckDto.color());

    List<CardDto> cards = deckDto.cards() == null ? List.of() : deckDto.cards();
    for (CardDto c : cards) {
      Card card = new Card(c.front(), c.back());
      card.setExample(c.example());
      card.setPronunciation(c.pronunciation());
      card.setNotes(c.notes());
      // Unrecognized / null already mapped to NEW by LearningState.fromJson (FB-1); guard anyway.
      card.setLearningState(c.learningState() == null ? LearningState.NEW : c.learningState());
      // Preserve SRS + statistics verbatim — no SrsEngine (FP-2).
      card.setEaseFactor(c.easeFactor());
      card.setInterval(c.interval());
      card.setRepetitions(c.repetitions());
      if (c.nextReviewDate() != null) {
        card.setNextReviewDate(c.nextReviewDate());
      }
      card.setLastReviewedAt(c.lastReviewedAt());
      card.setTotalReviews(c.totalReviews());
      card.setCorrectReviews(c.correctReviews());
      // id / createdAt / updatedAt left at entity defaults (regenerated, FP-1).
      deck.addCard(card);
    }

    return deckRepository.save(deck); // cascade=ALL persists cards with the deck
  }

  // --- Helpers ----------------------------------------------------------------

  private Deck findDeck(UUID deckId) {
    return deckRepository
        .findById(deckId)
        .orElseThrow(() -> new NotFoundException("Deck not found: " + deckId));
  }

  /**
   * Deserializes and structurally validates (VR-1). Failure -&gt; {@link
   * InvalidImportFormatException}.
   */
  private DeckExportDto readExport(byte[] jsonBytes) {
    if (jsonBytes == null || jsonBytes.length == 0) {
      throw new InvalidImportFormatException("Empty import payload");
    }
    DeckExportDto dto;
    try {
      dto = mapper.readValue(jsonBytes, DeckExportDto.class);
    } catch (IOException e) {
      throw new InvalidImportFormatException("Invalid WordBook export format", e);
    }
    if (dto == null || dto.deck() == null || dto.deck().name() == null) {
      throw new InvalidImportFormatException("Invalid WordBook export: missing deck");
    }
    return dto;
  }

  private DeckExportDto toExportDto(Deck deck) {
    List<CardDto> cards = deck.getCards().stream().map(DeckImportExportService::toCardDto).toList();
    DeckDto deckDto =
        new DeckDto(
            deck.getId(),
            deck.getName(),
            deck.getDescription(),
            deck.getColor(),
            deck.getCreatedAt(),
            deck.getUpdatedAt(),
            cards);
    return new DeckExportDto("1.0", Instant.now(), deckDto);
  }

  private static CardDto toCardDto(Card c) {
    return new CardDto(
        c.getId(),
        c.getFront(),
        c.getBack(),
        c.getExample(),
        c.getPronunciation(),
        c.getNotes(),
        c.getLearningState(),
        c.getEaseFactor(),
        c.getInterval(),
        c.getRepetitions(),
        c.getNextReviewDate(),
        c.getLastReviewedAt(),
        c.getTotalReviews(),
        c.getCorrectReviews(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }
}
