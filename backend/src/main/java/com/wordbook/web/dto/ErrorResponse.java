package com.wordbook.web.dto;

import java.time.Instant;

/** Consistent error envelope returned by {@code GlobalExceptionHandler} (RD-3, ER-1/2). */
public record ErrorResponse(int status, String error, String message, Instant timestamp) {

  public static ErrorResponse of(int status, String error, String message) {
    return new ErrorResponse(status, error, message, Instant.now());
  }
}
