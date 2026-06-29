package com.wordbook.web;

import static org.hamcrest.Matchers.greaterThan;
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
 * End-to-end slice (MockMvc): create deck -> add card -> start study -> answer -> nextReviewDate
 * updates. Also covers validation (400) and not-found (404) error paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WalkingSkeletonIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void fullSliceCreateToAnswer() throws Exception {
    // 1. Create a deck.
    MvcResult deckResult =
        mockMvc
            .perform(
                post("/api/decks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"English\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("English"))
            .andReturn();
    String deckId =
        objectMapper.readTree(deckResult.getResponse().getContentAsString()).get("id").asText();

    // 2. Add a card.
    MvcResult cardResult =
        mockMvc
            .perform(
                post("/api/decks/" + deckId + "/cards")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"front\":\"apple\",\"back\":\"사과\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.learningState").value("new"))
            .andReturn();
    JsonNode cardNode = objectMapper.readTree(cardResult.getResponse().getContentAsString());
    String cardId = cardNode.get("id").asText();
    String originalNextReview = cardNode.get("nextReviewDate").asText();

    // 3. Start a study session.
    MvcResult studyResult =
        mockMvc
            .perform(post("/api/decks/" + deckId + "/study"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cards", hasSize(1)))
            .andReturn();
    String sessionId =
        objectMapper
            .readTree(studyResult.getResponse().getContentAsString())
            .get("sessionId")
            .asText();

    // 4. Submit an answer (rating wire format = integer 2 = GOOD).
    mockMvc
        .perform(
            post("/api/study/" + sessionId + "/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cardId\":\"" + cardId + "\",\"rating\":2,\"timeSpentMs\":3500}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interval").value(1))
        .andExpect(jsonPath("$.repetitions").value(1))
        .andExpect(jsonPath("$.totalReviews").value(1))
        // 5. nextReviewDate moved forward.
        .andExpect(jsonPath("$.nextReviewDate").value(greaterThan(originalNextReview)));
  }

  @Test
  void blankDeckNameReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/decks").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void studyUnknownDeckReturns404() throws Exception {
    mockMvc
        .perform(post("/api/decks/00000000-0000-0000-0000-000000000000/study"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }
}
