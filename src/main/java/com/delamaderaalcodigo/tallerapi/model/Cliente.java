package com.delamaderaalcodigo.tallerapi.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Cliente del taller de carpintería
 *
 * <p>Según el documento de requisitos, la gestión de Cliente se limita
 * a un CRUD básico: existe principalmente porque {@link Proyecto}
 * necesita una referencia a un cliente, no porque requiera lógica de
 * negocio propia en esta versión (V1).</p>
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /**
     * Email de contacto del cliente. Es único a nivel de base de datos
     * (Constrainte UNIKE en V1__create_schema.sql) y se valida su
     * formato {@link Email}.
     */
    @Email
    @Size(max = 150)
    @Column(name = "email", unique = true, length = 150)
    private String email;

    @Size(max = 20)
    @Column(name = "telefono", length = 20)
    private String telefono;
}
