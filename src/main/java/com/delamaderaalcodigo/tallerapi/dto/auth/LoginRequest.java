package com.delamaderaalcodigo.tallerapi.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo esperado en POST /api/v1/auth/login. */
public record LoginRequest(
        @NotBlank(message = "username es obligatorio") String username,
        @NotBlank(message = "password es obligatorio") String password
) {
}