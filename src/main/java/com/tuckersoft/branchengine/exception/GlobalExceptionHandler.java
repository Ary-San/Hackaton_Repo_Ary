package com.tuckersoft.branchengine.exception;

import com.tuckersoft.branchengine.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/** Todos los errores del sistema salen por aqui con el mismo formato. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> apiException(ApiException ex, HttpServletRequest req) {
        return responder(ex.getStatus(), ex.getError(), ex.getMessage(), req);
    }

    /** @Valid sobre el @RequestBody: se listan todos los campos que fallaron. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex,
                                                    HttpServletRequest req) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                detalle.isBlank() ? "Request invalido" : detalle, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> violacion(ConstraintViolationException ex,
                                                   HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), req);
    }

    /** JSON ilegible o un enum que no existe: tambien es un 400, no un 500. */
    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> cuerpoInvalido(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request invalido: " + ex.getMessage(), req);
    }

    /** Por si un AccessDeniedException llega hasta aqui en vez de al handler de Security. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex,
                                                        HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tienes permiso sobre este recurso", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> noAutenticado(AuthenticationException ex,
                                                       HttpServletRequest req) {
        return responder(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Credenciales invalidas", req);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(NoHandlerFoundException ex,
                                                         HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ruta no encontrada", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Error inesperado en {}", req.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Error interno del motor de ramas", req);
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String error,
                                                    String mensaje, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(error, mensaje, req.getRequestURI()));
    }
}
