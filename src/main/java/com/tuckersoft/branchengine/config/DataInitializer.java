package com.tuckersoft.branchengine.config;

import com.tuckersoft.branchengine.domain.Roles;
import com.tuckersoft.branchengine.domain.User;
import com.tuckersoft.branchengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Crea al administrador al arrancar, leyendo ADMIN_NAME / ADMIN_EMAIL / ADMIN_PASSWORD
 * del .env. Si ya existe un usuario con ese email, no hace nada.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.display-name}")
    private String displayName;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(email)) {
            log.info("El administrador {} ya existe: el DataInitializer no toca nada.", email);
            return;
        }

        User admin = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .displayName(displayName)
                .role(Roles.ADMIN)
                .createdAt(Instant.now())
                .build();

        userRepository.save(admin);
        log.info("Administrador creado: {}", email);
    }
}
