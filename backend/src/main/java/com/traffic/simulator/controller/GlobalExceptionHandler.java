package com.traffic.simulator.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
    if (isSseRequest(request)) {
      return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ex.getMessage());
    }

    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "error", ex.getMessage(),
                "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    if (isSseRequest(request)) {
      return ResponseEntity.badRequest()
          .contentType(MediaType.TEXT_PLAIN)
          .body("Invalid value for parameter: " + ex.getName());
    }

    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "error", "Invalid value for parameter: " + ex.getName(),
                "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleUnhandled(Exception ex, HttpServletRequest request) {
    if (isSseRequest(request)) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.TEXT_PLAIN)
          .body("Internal server error");
    }

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            Map.of(
                "error", "Internal server error",
                "timestamp", Instant.now().toString()));
  }

  private boolean isSseRequest(HttpServletRequest request) {
    String acceptHeader = request.getHeader("Accept");
    return acceptHeader != null && acceptHeader.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
  }
}
