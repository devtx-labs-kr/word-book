package com.wordbook.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** MockMvc coverage for U2 deck/card CRUD, list/filter/search, and 404/400/204 error paths. */
@SpringBootTest
@AutoConfigureMockMvc
class DeckCardManagementIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private String createDeck(String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/decks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + name + "\",\"descriptionText\":\"d\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.descriptionText").value("d"))
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
  }

  private String createCard(String deckId, String front, String back) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/decks/" + deckId + "/cards")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"front\":\""
                            + front
                            + "\",\"back\":\""
                            + back
                            + "\",\"pronunciation\":\"/p/\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.pronunciation").value("/p/"))
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
  }

  @Test
  void deckCrudLifecycle() throws Exception {
    String deckId = createDeck("CrudDeck");

    // GET detail
    mockMvc
        .perform(get("/api/decks/" + deckId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("CrudDeck"));

    // PUT update
    mockMvc
        .perform(
            put("/api/decks/" + deckId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"CrudDeckRenamed\",\"color\":\"#FF3B30\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("CrudDeckRenamed"))
        .andExpect(jsonPath("$.color").value("#FF3B30"));

    // DELETE -> 204
    mockMvc.perform(delete("/api/decks/" + deckId)).andExpect(status().isNoContent());

    // GET after delete -> 404
    mockMvc.perform(get("/api/decks/" + deckId)).andExpect(status().isNotFound());
  }

  @Test
  void deckListReturnsSummaryWithStats() throws Exception {
    String deckId = createDeck("StatsDeck-list");
    createCard(deckId, "a", "1");

    mockMvc
        .perform(get("/api/decks").param("search", "StatsDeck-list").param("sort", "name"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].totalCards").value(1))
        .andExpect(jsonPath("$[0].newCards").value(1))
        .andExpect(jsonPath("$[0].dueCards").value(greaterThanOrEqualTo(1)));
  }

  @Test
  void cardCrudAndFilterSearch() throws Exception {
    String deckId = createDeck("CardOps");
    String cardId = createCard(deckId, "apple", "사과");
    createCard(deckId, "banana", "바나나");

    // list all
    mockMvc
        .perform(get("/api/decks/" + deckId + "/cards").param("filter", "All"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));

    // search front substring
    mockMvc
        .perform(get("/api/decks/" + deckId + "/cards").param("search", "app"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].front").value("apple"));

    // filter New (both are new)
    mockMvc
        .perform(get("/api/decks/" + deckId + "/cards").param("filter", "New"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));

    // update card via /api/cards/{id}
    mockMvc
        .perform(
            put("/api/cards/" + cardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"front\":\"apricot\",\"back\":\"살구\",\"notes\":\"edited\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.front").value("apricot"))
        .andExpect(jsonPath("$.notes").value("edited"))
        // SRS state untouched (DR-5).
        .andExpect(jsonPath("$.learningState").value("new"));

    // delete card -> 204
    mockMvc.perform(delete("/api/cards/" + cardId)).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/decks/" + deckId + "/cards").param("filter", "All"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  void deleteUnknownDeckReturns404() throws Exception {
    mockMvc
        .perform(delete("/api/decks/00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void deleteUnknownCardReturns404() throws Exception {
    mockMvc
        .perform(delete("/api/cards/00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void updateDeckWithBlankNameReturns400() throws Exception {
    String deckId = createDeck("BlankNameDeck");
    mockMvc
        .perform(
            put("/api/decks/" + deckId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void malformedUuidPathVariableReturns400() throws Exception {
    mockMvc
        .perform(get("/api/decks/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }
}
