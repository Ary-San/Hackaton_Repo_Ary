package com.tuckersoft.branchengine.exception;

import org.springframework.http.HttpStatus;

/** Codigo repetido, o una partida que ya termino. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
