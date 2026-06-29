package com.wordbook.exception;

/**
 * Thrown when an imported JSON payload cannot be safely deserialized (SD-3 -> HTTP 400). Defined in
 * U1 for the shared GlobalExceptionHandler; the import flow itself lands in U5.
 */
public class InvalidImportFormatException extends RuntimeException {

  public InvalidImportFormatException(String message) {
    super(message);
  }

  public InvalidImportFormatException(String message, Throwable cause) {
    super(message, cause);
  }
}
