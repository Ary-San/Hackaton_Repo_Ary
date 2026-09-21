package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** No hay campo de usuario: el dueno sale del token. */
public record PlaythroughRequest(

        @NotBlank @Size(min = 2, max = 40)
        String playerTag,

        @NotBlank
        String startNodeCode) {
}
