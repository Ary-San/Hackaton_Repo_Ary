package com.tuckersoft.branchengine.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee "Authorization: Bearer &lt;token&gt;", valida la firma y deja la autenticacion en el
 * SecurityContextHolder.
 *
 * Si algo no cuadra (sin cabecera, sin el prefijo Bearer, firma rota, token vencido)
 * el filtro NO revienta: sigue la cadena sin autenticar y deja que el
 * AuthenticationEntryPoint responda el 401 con el formato del enunciado.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String cabecera = request.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String email = jwtService.emailDelToken(cabecera.substring(PREFIJO.length()).trim());
            if (email != null) {
                try {
                    // Las autoridades salen de aqui, es decir, de la base de datos.
                    UserDetails usuario = userDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            usuario, null, usuario.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception e) {
                    // El usuario del token ya no existe: se sigue sin autenticar.
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
