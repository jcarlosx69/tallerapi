package com.delamaderaalcodigo.tallerapi.exception;

import com.delamaderaalcodigo.tallerapi.model.EstadoProyecto;

/**
 * RN-03: se intenta mover un Proyecto a un estado al que no se puede
 * llegar desde su estado actual (p.ej. de ENTREGADO a EN_CURSO). Se
 * traduce a 400 Bad Request: el cliente está enviando una operación
 * que, dado el estado actual del recurso, nunca es válida — no es un
 * conflicto temporal como el del stock, es un error de la petición.
 */
public class TransicionEstadoInvalidaException extends RuntimeException {

    public TransicionEstadoInvalidaException(String message) {
        super(message);
    }

    public static TransicionEstadoInvalidaException de(EstadoProyecto actual, EstadoProyecto destino) {
        return new TransicionEstadoInvalidaException(
                "No se puede pasar del estado %s al estado %s".formatted(actual, destino));
    }
}
