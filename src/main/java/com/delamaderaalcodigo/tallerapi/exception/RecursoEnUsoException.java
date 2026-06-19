package com.delamaderaalcodigo.tallerapi.exception;

/**
 * RN-04: se intenta eliminar un Cliente con Proyectos asociados, o un
 * Material con asignaciones activas en algún Proyecto. Se traduce a
 * 409 Conflict: el recurso existe y la petición está bien formada,
 * pero su estado actual (tiene dependencias) impide la operación.
 * <p>
 * Es genérica (no {@code ClienteEnUsoException} / {@code MaterialEnUsoException})
 * porque el motivo de conflicto es el mismo concepto en ambos casos —
 * "no se puede eliminar, tiene dependencias activas" — y el mensaje ya
 * deja claro de qué recurso y motivo se trata. Ver
 * {@code fase2c-notas-tecnicas.md}, sección 4, para la justificación
 * completa frente a duplicar la comprobación en cada servicio.
 */
public class RecursoEnUsoException extends RuntimeException {

    public RecursoEnUsoException(String message) {
        super(message);
    }

    public static RecursoEnUsoException paraId(String tipoRecurso, Long id, String motivo) {
        return new RecursoEnUsoException(
                "No se puede eliminar %s con id: %d (%s)".formatted(tipoRecurso, id, motivo));
    }
}
