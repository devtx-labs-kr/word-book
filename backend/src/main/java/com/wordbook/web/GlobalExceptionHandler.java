package com.wordbook.web;

import com.wordbook.exception.InvalidImportFormatException;
import com.wordbook.exception.NotFoundException;
import com.wordbook.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Surfaces errors as consistent JSON (ER-1 — never swallow errors silently). NotFound -> 404,
 * validation -> 400, malformed path variables (e.g. non-UUID id) -> 400, {@link
 * InvalidImportFormatException} -> 400, everything else -> 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .orElse("Validation failed");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of(400, "Bad Request", message));
  }

  /**
   * A path/query variable that cannot be bound to its target type (e.g. a malformed UUID id) is a
   * client error, not a server fault — surface it as 400 rather than letting it fall through to the
   * catch-all 500 (nfr-design logical-components review finding #1).
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String message = ex.getName() + ": invalid value '" + ex.getValue() + "'";
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of(400, "Bad Request", message));
  }

  @ExceptionHandler(InvalidImportFormatException.class)
  public ResponseEntity<ErrorResponse> handleInvalidImport(InvalidImportFormatException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
  }

  /**
   * A request body that cannot be deserialized — e.g. an out-of-range/unrecognized {@code rating}
   * integer that {@code ReviewRating.fromValue} rejects, which Jackson wraps as {@link
   * HttpMessageNotReadableException} — is a client error, surfaced as 400 (BR-ERR-3,
   * reliability-design RD-U3-4). Symmetric with the {@link MethodArgumentTypeMismatchException} →
   * 400 mapping above; without this the malformed body would fall through to the catch-all 500.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of(400, "Bad Request", "Malformed request body"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(500, "Internal Server Error", ex.getMessage()));
  }
}
