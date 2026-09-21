package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.UpdateRoleRequest;
import com.tuckersoft.branchengine.dto.UserResponse;
import com.tuckersoft.branchengine.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> yo() {
        return ResponseEntity.ok(userService.yo());
    }

    /** Solo ROLE_ADMIN: la regla vive en el SecurityFilterChain. */
    @GetMapping
    public ResponseEntity<List<UserResponse>> listar() {
        return ResponseEntity.ok(userService.listar());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> cambiarRol(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.cambiarRol(id, request.role()));
    }
}
