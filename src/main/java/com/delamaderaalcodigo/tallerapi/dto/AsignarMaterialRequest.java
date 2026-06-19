package com.delamaderaalcodigo.tallerapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO de entrada para {@code POST /api/v1/proyectos/{id}/materiales}.
 * {@code cantidadAsignada} usa {@code inclusive = false}: el documento
 * de requisitos especifica para {@code ProyectoMaterial.cantidadAsignada}
 * la restricciÃ³n "NOT NULL, &gt; 0" (estrictamente mayor que cero, a
 * diferencia de {@code stockDisponible} en Material, que sÃ­ admite 0).
 */
public record AsignarMaterialRequest (

        @NotNull(message = "El material es obligatorio")
        Long materialId,

        @NotNull(message = "La cantidad asignada es obligatoria")
        @DecimalMin(value = "0", inclusive = false, message = "La cantidad asignada debe ser mayor que 0")
        BigDecimal cantidadAsignada
) {
}
