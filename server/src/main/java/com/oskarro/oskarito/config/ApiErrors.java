package com.oskarro.oskarito.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Returns the human-readable reason of a {@link ResponseStatusException} as {"status", "message"}.
 * Those reasons are our own Polish messages written for users, so they are safe to show;
 * other exceptions keep Spring's default body without details.
 */
@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> status(ResponseStatusException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ex.getStatusCode().value());
        body.put("message", ex.getReason() == null ? "" : ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
}
