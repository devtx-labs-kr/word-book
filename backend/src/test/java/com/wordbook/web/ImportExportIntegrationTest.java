package com.wordbook.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** MockMvc coverage for U5 export/import endpoints: attachment headers, preview, 201, 400, 404. */
@SpringBootTest
@AutoConfigureMockMvc
class ImportExportIntegrationTest {

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
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
  }

  private byte[] sampleBytes() throws Exception {
    return new ClassPathResource("macos-sample-deck.json").getContentAsByteArray();
  }

  @Test
  void exportReturnsJsonAttachmentWithSuggestedFilename() throws Exception {
    String deckId = createDeck("ExportMe");

    mockMvc
        .perform(get("/api/decks/" + deckId + "/export"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(header().string("Content-Disposition", containsString("attachment")))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    matchesPattern(".*filename=\"ExportMe_\\d{4}-\\d{2}-\\d{2}\\.json\".*")))
        .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''")))
        .andExpect(jsonPath("$.version").value("1.0"))
        .andExpect(jsonPath("$.deck.name").value("ExportMe"));
  }

  @Test
  void exportUnknownDeckReturns404() throws Exception {
    mockMvc
        .perform(get("/api/decks/00000000-0000-0000-0000-000000000000/export"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void previewReportsValidDeck() throws Exception {
    mockMvc
        .perform(
            post("/api/decks/import/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleBytes()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true))
        .andExpect(jsonPath("$.deckName").value("기초 영어"))
        .andExpect(jsonPath("$.cardCount").value(2));
  }

  @Test
  void previewReportsInvalidForGarbage() throws Exception {
    mockMvc
        .perform(
            post("/api/decks/import/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not a wordbook file"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(false));
  }

  @Test
  void importCreatesDeckAndReturns201() throws Exception {
    mockMvc
        .perform(
            post("/api/decks/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleBytes()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("기초 영어"))
        .andExpect(jsonPath("$.totalCards").value(2));
  }

  @Test
  void importInvalidJsonReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/decks/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ this is not valid json "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void importStructurallyInvalidJsonReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/decks/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"1.0\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }
}
