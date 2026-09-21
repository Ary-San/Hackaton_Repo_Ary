package com.tuckersoft.branchengine.exception;

import org.springframework.http.HttpStatus;

/**
 * Login fallido.
 *
 * El mensaje es el mismo tanto si el email no existe como si la contrasena no
 * coincide: no se revela cual de las dos fallo.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Credenciales invalidas");
    }
}
