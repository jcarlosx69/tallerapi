package com.delamaderaalcodigo.tallerapi.service;

import com.delamaderaalcodigo.tallerapi.dto.ClienteResponse;
import com.delamaderaalcodigo.tallerapi.exception.RecursoEnUsoException;
import com.delamaderaalcodigo.tallerapi.exception.RecursoNoEncontradoException;
import com.delamaderaalcodigo.tallerapi.model.Cliente;
import com.delamaderaalcodigo.tallerapi.repository.ClienteRepository;
import com.delamaderaalcodigo.tallerapi.repository.ProyectoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link ClienteService}.
 *
 * <p>Esta clase ya existía conceptualmente desde Fase 2b, pero el método {@code eliminar()}
 * se modificó en Fase 2c para incorporar RN-04 (integridad referencial con Proyecto). Esta
 * sub-fase (2d-1) cubre tanto esa regla nueva como los casos de {@code obtenerPorId()} que
 * quedaron sin test explícito hasta ahora.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @InjectMocks
    private ClienteService clienteService;

    private static Cliente clienteDePrueba(Long id) {
        return Cliente.builder()
                .id(id)
                .nombre("Carpintería López")
                .email("contacto" + id + "@correo.com")
                .telefono("600000000")
                .build();
    }

    @Nested
    @DisplayName("eliminar() — RN-04: integridad referencial")
    class Eliminar {

        @Test
        @DisplayName("deberia_lanzarRecursoEnUso_cuandoElClienteTieneProyectosAsociados")
        void deberia_lanzarRecursoEnUso_cuandoElClienteTieneProyectosAsociados() {
            // --- Arrange ---
            Cliente cliente = clienteDePrueba(1L);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(proyectoRepository.existsByClienteId(1L)).thenReturn(true);

            // --- Act & Assert ---
            assertThatThrownBy(() -> clienteService.eliminar(1L))
                    .isInstanceOf(RecursoEnUsoException.class)
                    .hasMessageContaining("proyectos asociados");

            // Si la comprobación de RN-04 falla, la eliminación no debe llegar a ejecutarse.
            verify(clienteRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deberia_eliminarElCliente_cuandoNoTieneProyectosAsociados")
        void deberia_eliminarElCliente_cuandoNoTieneProyectosAsociados() {
            // Arrange
            Cliente cliente = clienteDePrueba(1L);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(proyectoRepository.existsByClienteId(1L)).thenReturn(false);

            // Act
            clienteService.eliminar(1L);

            // Assert
            verify(clienteRepository).delete(cliente);
        }

        @Test
        @DisplayName("deberia_lanzarRecursoNoEncontrado_cuandoElClienteAEliminarNoExiste")
        void deberia_lanzarRecursoNoEncontrado_cuandoElClienteAEliminarNoExiste() {
            // Arrange
            when(clienteRepository.findById(404L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> clienteService.eliminar(404L))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            // Si el cliente no existe, ni siquiera debe llegar a comprobarse RN-04.
            verify(proyectoRepository, never()).existsByClienteId(any());
        }
    }

    @Nested
    @DisplayName("obtenerPorId()")
    class ObtenerPorId {

        @Test
        @DisplayName("deberia_devolverElClienteMapeado_cuandoElIdExiste")
        void deberia_devolverElClienteMapeado_cuandoElIdExiste() {
            // Arrange
            Cliente cliente = clienteDePrueba(1L);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

            // Act
            ClienteResponse response = clienteService.obtenerPorId(1L);

            // Assert
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.nombre()).isEqualTo(cliente.getNombre());
            assertThat(response.email()).isEqualTo(cliente.getEmail());
        }

        @Test
        @DisplayName("deberia_lanzarRecursoNoEncontrado_cuandoElIdNoExiste")
        void deberia_lanzarRecursoNoEncontrado_cuandoElIdNoExiste() {
            // Arrange
            when(clienteRepository.findById(404L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> clienteService.obtenerPorId(404L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("404");
        }
    }
}
