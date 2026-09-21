package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.domain.Roles;
import com.tuckersoft.branchengine.domain.User;
import com.tuckersoft.branchengine.dto.UserResponse;
import com.tuckersoft.branchengine.exception.BadRequestException;
import com.tuckersoft.branchengine.exception.NotFoundException;
import com.tuckersoft.branchengine.repository.UserRepository;
import com.tuckersoft.branchengine.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticatedUser authenticatedUser;

    @Transactional(readOnly = true)
    public UserResponse yo() {
        return UserResponse.de(authenticatedUser.actual());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listar() {
        return userRepository.findAllByOrderByIdAsc().stream().map(UserResponse::de).toList();
    }

    @Transactional
    public UserResponse cambiarRol(Long id, String nuevoRol) {
        if (!Roles.VALIDOS.contains(nuevoRol)) {
            throw new BadRequestException("El rol debe ser ROLE_USER o ROLE_ADMIN");
        }

        User objetivo = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el usuario con id " + id));

        // Nadie se degrada a si mismo: el sistema se quedaria sin administradores.
        User actor = authenticatedUser.actual();
        if (objetivo.getId().equals(actor.getId())) {
            throw new BadRequestException("Un administrador no puede cambiar su propio rol");
        }

        objetivo.setRole(nuevoRol);
        userRepository.save(objetivo);

        return UserResponse.de(objetivo);
    }
}
