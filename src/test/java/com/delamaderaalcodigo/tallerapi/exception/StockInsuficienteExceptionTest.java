package com.delamaderaalcodigo.tallerapi.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

 class StockInsuficienteExceptionTest {

    @Test
    @DisplayName("deberia_mostrarCantidadesSinCerosSobrantes_cuandoElStockTieneEscalaDeBaseDeDatos")
    void deberia_mostrarCantidadesSinCerosSobrantes_cuandoElStockTieneEscalaDeBaseDeDatos() {
        // Arrange
        BigDecimal solicitado = new BigDecimal("50");
        BigDecimal disponible = new BigDecimal("10.000");

        // Act
        String mensaje = StockInsuficienteException.de("Tablero", solicitado, disponible).getMessage();

        // Assert
        assertThat(mensaje).isEqualTo(
                "Stock insuficiente para \"Tablero\": se solicitó 50 y el stock disponible es 10");
    }

    @Test
    @DisplayName("deberia_noUsarNotacionCientifica_cuandoLaCantidadEsUnEnteroAcabadoEnCero")
    void deberia_noUsarNotacionCientifica_cuandoLaCantidadEsUnEnteroAcabadoEnCero() {
        // Arrange
        BigDecimal solicitado = new BigDecimal("150");
        BigDecimal disponible = new BigDecimal("100");

        // Act
        String mensaje = StockInsuficienteException.de("Tablero", solicitado, disponible).getMessage();

        // Assert
        assertThat(mensaje)
                .endsWith("es 100");
    }
}
