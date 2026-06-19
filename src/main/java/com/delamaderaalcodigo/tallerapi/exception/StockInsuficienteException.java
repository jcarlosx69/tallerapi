package com.delamaderaalcodigo.tallerapi.exception;

import java.math.BigDecimal;

/**
 * RN-01: se intenta asignar a un proyecto más cantidad de un material
 * de la que hay disponible en stock. Se traduce a 409 Conflict en
 * {@link GlobalExceptionHandler} (no 400): no es que la petición esté
 * mal formada, es que entra en conflicto con el estado actual del
 * inventario — la misma petición podría aceptarse más tarde si llega
 * stock nuevo.
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String message) {
        super(message);
    }

    public static StockInsuficienteException de(String materialNombre, BigDecimal solicitado, BigDecimal disponible) {
        return new StockInsuficienteException(
                "Stock insuficiente para \"%s\": se solicitó %s y el stock disponible es %s"
                        .formatted(materialNombre, solicitado, disponible));
    }
}
