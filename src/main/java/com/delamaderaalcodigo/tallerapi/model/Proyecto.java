package com.delamaderaalcodigo.tallerapi.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Encargo de carpinteríia gestionado por el taller
 *
 * <p>Cada proyecto pertenece a un {@link Cliente}({@code @MayToOne},
 * relación N:1 desde el punto de vista del Proyecto) y puede tener
 * asociados varios materiales a traver del {@link ProyectoMaterial}.</p>
 *
 * <p>El campo {@code estado} controla el ciclo de vida del proyecto
 * según {@link EstadoProyecto}; las transiciones válidad (RN-03) se
 * validarán en la capa de servicio en la Fase 2.</p>
 */

@Entity
@Table(name = "proyectos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 150)
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    /**
     * Cliente para el que se realiza el proyecto
     *
     * <p>Se usa {@code FetchType.LAZY} como buena práctica por defecto
     * en relaciones {@code @ManyToOne}: evita cargar el Cliente
     * completo cada vez que se recupera un Proyecto si no es
     * necesario (por ejemplo, en un listado paginado</p>
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /**
     * Fecha de entrega prevista. Opcional, peri si se especifica debe
     * ser posterior o igual a {@code fechaInicio}(RN-06). Esa
     * validación cruzada entre campos se implementará en la capa de servicio/DTO
     * en la Fase 2.
     */
    @Column(name = "fecha_entrega_prevista")
    private LocalDate fechaEntregaPrevista;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoProyecto estado;

    @DecimalMin(value = "0", inclusive = true)
    @Column(name = "presupuesto", precision = 12, scale = 2)
    private BigDecimal presupuesto;
}
