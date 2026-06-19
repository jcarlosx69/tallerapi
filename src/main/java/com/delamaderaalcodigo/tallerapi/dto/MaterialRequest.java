package com.delamaderaalcodigo.tallerapi.dto;

import com.delamaderaalcodigo.tallerapi.model.TipoMaterial;
import com.delamaderaalcodigo.tallerapi.model.UnidadMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MaterialRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotNull(message = "El tipo de material es obligatorio")
        TipoMaterial tipo,

        @NotNull(message = "La unidad de medida es obligatoria")
        UnidadMedida unidad,

        @NotNull(message = "El stock disponible es obligatorio")
        @DecimalMin(value = "0", inclusive = true, message = "El stock disponible no puede ser negativo")
        BigDecimal stockDisponible,

        @NotNull(message = "El coste unitario es obligatorio")
        @DecimalMin(value = "0", inclusive = true, message = "El coste unitario no puede ser negativo")
        BigDecimal costeUnitario
) {
}