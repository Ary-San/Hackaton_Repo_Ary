package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.domain.Roles;
import com.tuckersoft.branchengine.domain.User;
import com.tuckersoft.branchengine.dto.AuthResponse;
import com.tuckersoft.branchengine.dto.LoginRequest;
import com.tuckersoft.branchengine.dto.RegisterRequest;
import com.tuckersoft.branchengine.exception.ConflictException;
import com.tuckersoft.branchengine.exception.InvalidCredentialsException;
import com.tuckersoft.branchengine.repository.UserRepository;
import com.tuckersoft.branchengine.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /** El rol se fija aqui, nunca se copia del request: siempre ROLE_USER. */
    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Ya existe un analista con el email " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .role(Roles.USER)
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);

        return AuthResponse.bearer(jwtService.generarToken(user.getEmail()),
                user.getEmail(), user.getDisplayName(), user.getRole());
    }

    /**
     * Un email inexistente y una contrasena incorrecta dan el mismo 401: no se revela
     * cual de las dos fallo.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        return AuthResponse.bearer(jwtService.generarToken(user.getEmail()),
                user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
