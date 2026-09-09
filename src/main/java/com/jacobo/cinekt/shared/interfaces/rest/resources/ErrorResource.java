package com.jacobo.cinekt.shared.interfaces.rest.resources;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform error body for every failure handled by {@code GlobalExceptionHandler}.
 * {@code fieldErrors} is omitted from the JSON unless the failure is a validation one.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResource(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {
}
