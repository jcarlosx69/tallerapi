package com.delamaderaalcodigo.tallerapi.dto;

import com.delamaderaalcodigo.tallerapi.model.EstadoProyecto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


/**
 * DTO de salida para Proyecto.
 * <p>
 * El campo {@code materiales} solo se rellena en el detalle
 * ({@code GET /api/v1/proyectos/{id}}); en el listado paginado
 * ({@code GET /api/v1/proyectos}) viaja como lista vacía a propósito,
 * para no disparar una consulta adicional por cada fila de la página
 * (problema N+1). Ver {@code ProyectoMapper} y
 * {@code fase2c-notas-tecnicas.md}, sección 5, para la justificación
 * completa de esta separación entre {@code aResponse} y
 * {@code aResponseDetalle}.
 */
public record ProyectoResponse(
        Long id,
        String  nombre,
        Long clienteId,
        String clienteNombre,
        LocalDate fechaInicio,
        LocalDate fechaEntregaPrevista,
        EstadoProyecto estado,
        BigDecimal presupuesto,
        List<ProyectoMaterialResponse> materiales
) {
}
