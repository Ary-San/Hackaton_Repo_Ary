package com.tuckersoft.branchengine.security;

import com.tuckersoft.branchengine.domain.Roles;
import com.tuckersoft.branchengine.domain.User;
import com.tuckersoft.branchengine.exception.InvalidCredentialsException;
import com.tuckersoft.branchengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Atajo para sacar del contexto de seguridad el User de verdad, no solo su nombre. */
@Component
@RequiredArgsConstructor
public class AuthenticatedUser {

    private final UserRepository userRepository;

    public User actual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new InvalidCredentialsException();
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(InvalidCredentialsException::new);
    }

    public static boolean esAdmin(User user) {
        return Roles.ADMIN.equals(user.getRole());
    }
}
