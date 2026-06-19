package com.delamaderaalcodigo.tallerapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.delamaderaalcodigo.tallerapi.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Se invoca cuando una request SÍ está autenticada (el JWT es válido) pero el usuario no
 * tiene el rol necesario, p.ej. un USER intentando hacer POST /api/v1/materiales.
 *
 * Distinguir 401 (no autenticado, CustomAuthenticationEntryPoint) de 403 (autenticado pero
 * sin permiso, este handler) es importante para que quien consume la API sepa si el
 * problema es "tu token" o "tu rol".
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Acceso denegado",
                "Tu usuario está autenticado pero no tiene el rol necesario para esta operación",
                request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}