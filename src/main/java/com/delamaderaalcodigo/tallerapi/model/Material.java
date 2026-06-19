package com.delamaderaalcodigo.tallerapi.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * Material disponible en el inventerio del taller (madera, herrajes,
 * barnices, etc).
 *
 * <p>{@code stockDisponible} es el campo central de la lǵica de
 * negocio de la Fase 2: se descuenta al asignar material a un
 * {@link Proyecto} (RN-01) y se repone al elimniar una asignación
 * (RN-02). Se modela como {@link BigDecimal} porque algunas unidades
 * (METRO, KG, LITRO) admiten cantidades fraccionadas.</p>
 */
@Entity
@Table(name = "materiales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoMaterial tipo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "unidad", nullable = false,length = 20)
    private UnidadMedida unidad;

    /**
     * cantidad acutalmente disponible en inventario, expresada en
     * {@code unidad}. Restringido a valores &gt:= 0 mediante
     * {@link DedimalMin} a nivel de aplicación y mediante
     * {@code CHECK} a nivel de datos.
     */
    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    @Column(name = "stock_disponible", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockDisponible;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    @Column(name = "coste_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal costeUnitario;
}
