package com.delamaderaalcodigo.tallerapi.dto;

/**
 * DTO de salida para Cliente. A diferencia de la entidad, expone
 * exactamente los campos que queremos publicar en la API (en este
 * caso coinciden con la entidad, pero no tiene por qué ser siempre
 * así — ver fase2b-notas-tecnicas.md, sección 1).
 */
public record ClienteResponse(
        Long id,
        String nombre,
        String email,
        String telefono
) {
}