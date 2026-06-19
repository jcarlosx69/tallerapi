package com.delamaderaalcodigo.tallerapi.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Asignación de un {@link Material} a un {@link Proyecto}.
 *
 * <p>Esta entidad representa la relación N:M entre Proyecto y
 * Material, pero <strong>no es una simple tabla intermedia</strong>:
 * tiene atributos propios ({@code cantidadAsignada},
 * {@code fechaAsignacio}) que son el núcleo de la lógica de negocio
 * de la Fase 2 (Validación y reposición de stock, RN-01/RN-02). Por
 * eso se modela como entidad de primer nivel con dos relaciones
 * {@code @ManyYoOne}, en lugar de usar {@code ManyToMany} de Spring
 * Data, que no permitiría añadir estos campos de forma natural.</p>
 */
@Entity
@Table(name = "proyecto_materiales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProyectoMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    /**
     * Cantidad de {@code material} consumida/asignada al proyecto, en
     * la unidad definida por {@code material.unidad}. Debe ser
     * estrictamente mayor que cero
     */
    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    @Column(name = "cantidad_asignada", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadAsignada;

    /**
     * Fecha y hora en que se realizó la asignación. Se autogenera en
     * el momento de la inserción (ver V1__create_schema.sql,
     * {@code DEFAULT now()});  no se expone como campo editable en la
     * API
     */
    @NotNull
    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;
}
