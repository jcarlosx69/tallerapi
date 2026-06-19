package com.delamaderaalcodigo.tallerapi.security.jwt;

import com.delamaderaalcodigo.tallerapi.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Se ejecuta UNA vez por request (de ahí OncePerRequestFilter) y se registra antes del
 * filtro estándar de usuario/contraseña de Spring (ver SecurityConfig).
 *
 * Su única responsabilidad: si la request trae un Bearer token válido, deja al usuario
 * autenticado en el SecurityContext para que el resto de la cadena (y el controller) lo
 * vean como "ya autenticado".
 *
 * Importante: si NO hay token, o es inválido, este filtro NO lanza ningún error y deja
 * pasar la request sin autenticar. Son las reglas de authorizeHttpRequests (en
 * SecurityConfig) las que decidirán si esa ruta concreta requiere autenticación. Esto
 * evita que el filtro tenga que conocer la lista de rutas públicas (login, swagger), que
 * ya está centralizada en un único sitio.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && jwtService.isTokenValid(token)) {
            String username = jwtService.extractUsername(token);

            // Defensa simple: si por lo que sea ya hubiera una autenticación en este
            // SecurityContext, no la pisamos.
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_NAME);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}