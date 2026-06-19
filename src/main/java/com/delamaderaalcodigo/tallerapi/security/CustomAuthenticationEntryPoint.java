package com.delamaderaalcodigo.tallerapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.delamaderaalcodigo.tallerapi.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Se invoca cuando una request SIN autenticar intenta acceder a un recurso protegido
 * (p.ej. GET /api/v1/proyectos sin header Authorization, o con un token inválido/caducado).
 *
 * Sin este componente, Spring Security devuelve por defecto una página HTML de error de
 * Tomcat, que no tiene ningún sentido en una API REST consumida por Swagger/Postman.
 *
 * Nota didáctica: este es un escenario distinto al de "login con contraseña incorrecta".
 * Aquí el usuario ya estaba intentando usar un recurso protegido sin credenciales válidas;
 * el fallo de login en sí (POST /auth/login con password incorrecta) se gestiona en
 * AuthController con su propio @ExceptionHandler, porque ahí el origen del error es
 * AuthenticationManager.authenticate(...), no un intento de acceso a un recurso protegido.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }


    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException{
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "No autenticado",
                "Se requiere un token JWT válido en el header Authorization (Bearer <token>)",
                request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
