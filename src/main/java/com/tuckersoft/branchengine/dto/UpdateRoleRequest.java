package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateRoleRequest(

        @NotBlank
        @Pattern(regexp = "ROLE_USER|ROLE_ADMIN", message = "El rol debe ser ROLE_USER o ROLE_ADMIN")
        String role) {
}
