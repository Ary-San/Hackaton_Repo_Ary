package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * handlerUnit y outcomeCode no aparecen aqui a proposito: se derivan del branchType,
 * que a su vez lo calcula el motor a partir del rawInput.
 */
public record DecisionRequest(

        @NotNull
        Long playthroughId,

        @NotBlank @Size(min = 10)
        String rawInput,

        @NotBlank
        @Pattern(regexp = "LEVE|MODERADO|GRAVE|CRITICO",
                message = "impactLevel debe ser LEVE, MODERADO, GRAVE o CRITICO")
        String impactLevel) {
}
