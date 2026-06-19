package com.delamaderaalcodigo.tallerapi.dto;

import java.time.Instant;

/**
 * Cuerpo JSON consistente para los errores de seguridad (401/403) de esta fase. Se diseña
 * deliberadamente genérico (timestamp/status/error/message/path) para poder reutilizarlo
 * más adelante con los errores 400/404/409 que pide el documento de requisitos (RN-01,
 * RN-03, RN-04...), de modo que el cliente de la API siempre recibe la misma forma de error
 * sin importar de qué capa venga.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
