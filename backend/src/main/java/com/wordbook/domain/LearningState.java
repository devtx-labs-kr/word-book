package com.wordbook.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Card learning lifecycle state. Ported 1:1 from the Swift {@code LearningState} enum.
 *
 * <p>JSON serialization uses the lowercase {@code rawValue} (new/learning/mastered/relearning) for
 * compatibility with the original macOS export format.
 */
public enum LearningState {
  NEW("new"),
  LEARNING("learning"),
  MASTERED("mastered"),
  RELEARNING("relearning");

  private final String jsonValue;

  LearningState(String jsonValue) {
    this.jsonValue = jsonValue;
  }

  /** Lowercase wire value emitted by Jackson. */
  @JsonValue
  public String getJsonValue() {
    return jsonValue;
  }

  /**
   * Resolves a {@link LearningState} from its lowercase wire value. Unrecognized values fall back
   * to {@link #NEW} (import-compatibility rule, see business-rules / U5).
   */
  @JsonCreator
  public static LearningState fromJson(String value) {
    if (value == null) {
      return NEW;
    }
    for (LearningState state : values()) {
      if (state.jsonValue.equalsIgnoreCase(value)) {
        return state;
      }
    }
    return NEW;
  }
}
