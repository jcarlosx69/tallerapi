package com.delamaderaalcodigo.tallerapi.dto.auth;

import java.time.Instant;

/** Respuesta de un login correcto: el JWT y el instante exacto en que caduca. */
public record LoginResponse(
        String token,
        Instant expiresAt
) {
}