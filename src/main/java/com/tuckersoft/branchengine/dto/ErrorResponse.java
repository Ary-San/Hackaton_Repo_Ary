package com.tuckersoft.branchengine.dto;

import java.time.Instant;

/** El unico formato de error del sistema, incluidos los 401 y 403 de Spring Security. */
public record ErrorResponse(String error, String message, Instant timestamp, String path) {

    public static ErrorResponse of(String error, String message, String path) {
        return new ErrorResponse(error, message, Instant.now(), path);
    }
}
