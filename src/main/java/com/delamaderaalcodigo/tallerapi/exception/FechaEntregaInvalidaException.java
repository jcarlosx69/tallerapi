package com.delamaderaalcodigo.tallerapi.exception;

/**
 * RN-06: {@code fechaEntregaPrevista}, cuando se especifica, debe ser
 * posterior o igual a {@code fechaInicio}. Se traduce a 400 Bad
 * Request: es un error de validación de los datos de entrada, aunque
 * por ser una comparación entre dos campos del propio DTO se resuelve
 * en el servicio en lugar de con una anotación de Bean Validation
 * (ver {@code ProyectoRequest}).
 */
public class FechaEntregaInvalidaException extends RuntimeException {

    public FechaEntregaInvalidaException(String message) {
        super(message);
    }
}
