package com.wordbook.exception;

/** Thrown when a requested resource does not exist (ER-2 -> HTTP 404). */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
