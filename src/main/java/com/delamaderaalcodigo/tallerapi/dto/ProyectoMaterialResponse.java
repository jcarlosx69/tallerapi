package com.delamaderaalcodigo.tallerapi.dto;

import com.delamaderaalcodigo.tallerapi.model.UnidadMedida;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida para una fila de {@code ProyectoMaterial}. Incluye
 * {@code materialNombre} y {@code unidad} (datos del Material, no de
 * la relación) para que un cliente de la API no tenga que hacer una
 * segunda llamada a {@code GET /api/v1/materiales/{id}} solo para
 * mostrar de forma legible qué se asignó.
 */
public record ProyectoMaterialResponse(
        Long id,
        Long materialId,
        String materialNombre,
        UnidadMedida unidad,
        BigDecimal cantidadAsignada,
        LocalDateTime fechaAsignacion
) {
}
