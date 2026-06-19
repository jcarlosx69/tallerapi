package com.delamaderaalcodigo.tallerapi.exception;

/**
 * Excepción de negocio para "no existe un recurso con este id". Se
 * traduce a 404 en {@link GlobalExceptionHandler}. Al extender
 * {@link RuntimeException} (no checked), no obliga a declarar
 * {@code throws} en cada método de servicio/repositorio.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }

    /**
     * Factoría de conveniencia para el caso más habitual, evitando
     * repetir la construcción del mensaje en cada servicio.
     */
    public static RecursoNoEncontradoException paraId(String tipoRecurso, Long id) {
        return new RecursoNoEncontradoException(
                "No existe %s con id: %d".formatted(tipoRecurso, id));
    }
}