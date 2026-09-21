package com.tuckersoft.branchengine.exception;

import org.springframework.http.HttpStatus;

/** El usuario esta autenticado, pero el recurso no es suyo o no le corresponde. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
