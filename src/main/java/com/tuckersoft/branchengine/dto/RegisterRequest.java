package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registro de un analista.
 *
 * No hay campo de rol a proposito: quien se registra queda siempre como ROLE_USER, y
 * un "role" que venga en el JSON se ignora sin mas.
 */
public record RegisterRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 6)
        String password,

        @NotBlank @Size(min = 3, max = 60)
        String displayName) {
}
