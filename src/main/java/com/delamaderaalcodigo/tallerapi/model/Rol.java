package com.delamaderaalcodigo.tallerapi.model;

/**
 * Rol de un {@link Usuario} dentro del sistema.
 *
 * <p>Según RN-05, las operaciones de escritura (POST/PUT/DELETE)
 * requieren un rol {@code ADMIN}; las operaciones de lectura (GET)
 * requieren autenticación pero permiten también el rol {@code USER}.
 * La aplicación concreta de estas reglas con Spring Security se hará en la
 * Fase 2, pero el modelo de datos ya queda preparado.</p>
 */
public enum Rol {
    ADMIN,
    USER
}
