package com.delamaderaalcodigo.tallerapi.model;


/**
 * Tipo de material gestionado en el inventario del taller
 *
 * <p>Se modela como enum (en lugar de String libre) para garantizar
 * consistencia de datos y permitir validación a nivel de base de datos
 * mediante un {@code CHECK} en la migracijón Flyway correspondiente.</p>
 */
public enum TipoMaterial {
    MADERA,
    HERRAJE,
    BARNIZ,
    OTRO
}
