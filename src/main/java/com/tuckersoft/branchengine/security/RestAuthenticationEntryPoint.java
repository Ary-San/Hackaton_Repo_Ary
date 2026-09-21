package com.tuckersoft.branchengine.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuckersoft.branchengine.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring Security devuelve los 401 con el cuerpo vacio. Esto los reescribe con el
 * formato de error del enunciado.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse cuerpo = ErrorResponse.of("UNAUTHORIZED",
                "Se requiere un token valido en la cabecera Authorization",
                request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), cuerpo);
    }
}
