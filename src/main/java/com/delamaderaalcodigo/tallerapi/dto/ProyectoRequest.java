package com.delamaderaalcodigo.tallerapi.dto;

import com.delamaderaalcodigo.tallerapi.model.EstadoProyecto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProyectoRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede tener más de 150 caracteres")
        String nombre,

        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        @NotNull(message = "la fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        /**
         * Opcional: si se especifica, NP-06 exige que sea >= fechaInicio.
         * Esa comparación cruzada entre dos campos del propio DTO no se
         * expresa bien con una sola anotación de Bean Validation sin
         * recurrir a un validador a medida; se resuelve en el servicio
         * (ProyectoService.validarFechas), igual que en el resto de reglas
         * de negocio RN-01 a RN-06, por consistencia.
         */
        LocalDate fechaEntregaPrevista,

        @NotNull(message = "El estado es obligatorio")
        EstadoProyecto estado,

        @DecimalMin(value = "0", inclusive = true, message = "El presupuesto no puede ser negativo")
        BigDecimal presupuesto
) {
}
