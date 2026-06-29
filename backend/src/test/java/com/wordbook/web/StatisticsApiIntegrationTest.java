package com.wordbook.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * MockMvc coverage for U4 statistics endpoints: GET /api/statistics returns 200 with the four
 * sections even when there is no data (zeros / empty list, BR-ST7); GET card-stats returns 200 for
 * an existing deck and 404 for an unknown deck (BR-ST11, GlobalExceptionHandler).
 */
@SpringBootTest
@AutoConfigureMockMvc
class StatisticsApiIntegrationTest {

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

  @Test
  void statisticsReturns200WithAllSectionsEvenWhenEmpty() throws Exception {
    // No assumption about prior data — assert the response shape and that empty is a 200, not 4xx.
    mockMvc
        .perform(get("/api/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.today").exists())
        .andExpect(jsonPath("$.today.cardsReviewed").isNumber())
        .andExpect(jsonPath("$.today.accuracy").isNumber())
        .andExpect(jsonPath("$.overall.totalCards").isNumber())
        .andExpect(jsonPath("$.upcoming.dueToday").isNumber())
        .andExpect(jsonPath("$.recentSessions").isArray());
  }

  @Test
  void cardStatsReturns200ForExistingDeck() throws Exception {
    String deckId = createDeck("CardStatsDeck");
    mockMvc
        .perform(
            post("/api/decks/" + deckId + "/cards")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"front\":\"apple\",\"back\":\"사과\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/decks/" + deckId + "/card-stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].word").value("apple"))
        .andExpect(jsonPath("$[0].accuracy").value(0.0));
  }

  @Test
  void cardStatsForEmptyDeckReturns200WithEmptyList() throws Exception {
    String deckId = createDeck("EmptyCardStatsDeck");
    mockMvc
        .perform(get("/api/decks/" + deckId + "/card-stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void cardStatsUnknownDeckReturns404() throws Exception {
    mockMvc
        .perform(get("/api/decks/00000000-0000-0000-0000-000000000000/card-stats"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }
}
