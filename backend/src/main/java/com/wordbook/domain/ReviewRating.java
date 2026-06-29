package com.wordbook.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * User rating of a card review. Ported 1:1 from the Swift {@code ReviewRating} enum.
 *
 * <p>Wire format is the integer value 0-3 (AGAIN=0, HARD=1, GOOD=2, EASY=3); the integer is
 * preserved across JSON.
 */
public enum ReviewRating {
  AGAIN(0),
  HARD(1),
  GOOD(2),
  EASY(3);

  private final int value;

  ReviewRating(int value) {
    this.value = value;
  }

  /** Integer wire value (0-3). */
  @JsonValue
  public int getValue() {
    return value;
  }

  /** Resolves a {@link ReviewRating} from its integer wire value (0-3). */
  @JsonCreator
  public static ReviewRating fromValue(int value) {
    for (ReviewRating rating : values()) {
      if (rating.value == value) {
        return rating;
      }
    }
    throw new IllegalArgumentException("Unknown ReviewRating value: " + value);
  }
}
