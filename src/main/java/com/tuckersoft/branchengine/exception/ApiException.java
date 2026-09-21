package com.tuckersoft.branchengine.exception;

import org.springframework.http.HttpStatus;

/** Base de los errores de negocio: cada uno ya sabe con que codigo HTTP sale. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    protected ApiException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
