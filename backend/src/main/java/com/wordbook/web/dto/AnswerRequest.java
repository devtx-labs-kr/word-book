package com.wordbook.web.dto;

import com.wordbook.domain.ReviewRating;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

/**
 * Request to submit an answer. {@code rating} deserializes from the integer wire value 0-3
 * (AGAIN=0/HARD=1/GOOD=2/EASY=3); {@code timeSpentMs} is milliseconds.
 */
public class AnswerRequest {

  @NotNull(message = "cardId is required")
  private UUID cardId;

  @NotNull(message = "rating is required")
  private ReviewRating rating;

  @PositiveOrZero(message = "timeSpentMs must be zero or positive")
  private long timeSpentMs;

  public UUID getCardId() {
    return cardId;
  }

  public void setCardId(UUID cardId) {
    this.cardId = cardId;
  }

  public ReviewRating getRating() {
    return rating;
  }

  public void setRating(ReviewRating rating) {
    this.rating = rating;
  }

  public long getTimeSpentMs() {
    return timeSpentMs;
  }

  public void setTimeSpentMs(long timeSpentMs) {
    this.timeSpentMs = timeSpentMs;
  }
}
