package com.wordbook.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * MockMvc coverage for U3 study completion: complete -> 200 summary, missing session -> 404,
 * out-of-range rating body -> 400 (HttpMessageNotReadableException handler, RD-U3-4), empty-session
 * start (0 cards) is OK with total=0.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StudyCompletionIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private String createDeck(String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/decks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + name + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
  }

  private String createCard(String deckId, String front, String back) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/decks/" + deckId + "/cards")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"front\":\"" + front + "\",\"back\":\"" + back + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
  }

  private String startSession(String deckId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/api/decks/" + deckId + "/study"))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .get("sessionId")
        .asText();
  }

  @Test
  void completeReturnsSummaryFromAccumulatedAnswers() throws Exception {
    String deckId = createDeck("CompleteDeck");
    String cardId = createCard(deckId, "apple", "사과");

    MvcResult studyResult =
        mockMvc
            .perform(post("/api/decks/" + deckId + "/study"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cards", hasSize(1)))
            .andExpect(jsonPath("$.total").value(1))
            .andReturn();
    JsonNode start = objectMapper.readTree(studyResult.getResponse().getContentAsString());
    String sessionId = start.get("sessionId").asText();

    mockMvc
        .perform(
            post("/api/study/" + sessionId + "/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cardId\":\"" + cardId + "\",\"rating\":2,\"timeSpentMs\":5000}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/study/" + sessionId + "/complete"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cardsReviewed").value(1))
        .andExpect(jsonPath("$.correctAnswers").value(1))
        .andExpect(jsonPath("$.accuracy").value(1.0))
        .andExpect(jsonPath("$.totalTimeSpent").value(5.0));
  }

  @Test
  void completeIsIdempotent() throws Exception {
    String deckId = createDeck("IdempotentDeck");
    createCard(deckId, "f", "b");
    String sessionId = startSession(deckId);

    mockMvc.perform(post("/api/study/" + sessionId + "/complete")).andExpect(status().isOk());
    // A second complete returns the same summary (no error).
    mockMvc
        .perform(post("/api/study/" + sessionId + "/complete"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cardsReviewed").value(0));
  }

  @Test
  void completeUnknownSessionReturns404() throws Exception {
    mockMvc
        .perform(post("/api/study/00000000-0000-0000-0000-000000000000/complete"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void outOfRangeRatingReturns400() throws Exception {
    String deckId = createDeck("BadRatingDeck");
    String cardId = createCard(deckId, "apple", "사과");
    String sessionId = startSession(deckId);

    // rating=9 is out of the 0-3 range; ReviewRating.fromValue throws, Jackson wraps it as
    // HttpMessageNotReadableException -> handled as 400 (RD-U3-4).
    mockMvc
        .perform(
            post("/api/study/" + sessionId + "/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cardId\":\"" + cardId + "\",\"rating\":9,\"timeSpentMs\":1000}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void emptySessionStartIsOkWithZeroCards() throws Exception {
    String deckId = createDeck("EmptyDeck");
    // No cards added.
    mockMvc
        .perform(post("/api/decks/" + deckId + "/study"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards", hasSize(0)))
        .andExpect(jsonPath("$.total").value(0));
  }
}
