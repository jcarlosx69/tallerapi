package com.delamaderaalcodigo.tallerapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Usuario de la aplicación, usado para la autenticacion JWT (Fase 2)
 *
 * <p>La contraseña nunca se almacena en texto plano: el campo
 * {@code passwordHash} contiene el hash BCrypt. El usuario
 * administrador inicial se crea mediante la migración
 * {@code V2__seed_admin_user.sql} (ver notas técnicas para el detalle
 * de cómo se generó ese hash)</p>
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Hash BCrypt de la contraseña. Nunca se debe asignar in leer
     * texto plano en este campo.
     */
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;
}
