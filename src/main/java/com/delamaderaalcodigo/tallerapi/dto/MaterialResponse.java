package com.delamaderaalcodigo.tallerapi.dto;

import com.delamaderaalcodigo.tallerapi.model.TipoMaterial;
import com.delamaderaalcodigo.tallerapi.model.UnidadMedida;

import java.math.BigDecimal;

public record MaterialResponse(
        Long id,
        String nombre,
        TipoMaterial tipo,
        UnidadMedida unidad,
        BigDecimal stockDisponible,
        BigDecimal costeUnitario
) {
}