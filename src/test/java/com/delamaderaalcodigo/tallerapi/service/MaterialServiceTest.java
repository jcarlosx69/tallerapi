package com.delamaderaalcodigo.tallerapi.service;

import com.delamaderaalcodigo.tallerapi.dto.MaterialResponse;
import com.delamaderaalcodigo.tallerapi.exception.RecursoEnUsoException;
import com.delamaderaalcodigo.tallerapi.exception.RecursoNoEncontradoException;
import com.delamaderaalcodigo.tallerapi.model.Material;
import com.delamaderaalcodigo.tallerapi.model.ProyectoMaterial;
import com.delamaderaalcodigo.tallerapi.model.TipoMaterial;
import com.delamaderaalcodigo.tallerapi.model.UnidadMedida;
import com.delamaderaalcodigo.tallerapi.repository.MaterialRepository;
import com.delamaderaalcodigo.tallerapi.repository.ProyectoMaterialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link MaterialService}.
 *
 * <p>Igual que {@code ClienteServiceTest}, esta clase cubre la regla RN-04 incorporada en
 * Fase 2c sobre {@code eliminar()}. A diferencia de {@code ClienteService}, aquí la
 * comprobación reutiliza un método de repositorio que ya existía desde Fase 1
 * ({@code findByMaterialId}), no uno añadido a propósito para esta regla — por eso no hace
 * falta verificar la firma de ningún método nuevo, solo su uso correcto en el servicio.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialService")
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private ProyectoMaterialRepository proyectoMaterialRepository;

    @InjectMocks
    private MaterialService materialService;

    private static Material materialDePrueba(Long id) {
        return Material.builder()
                .id(id)
                .nombre("Tablero de pino")
                .tipo(TipoMaterial.MADERA)
                .unidad(UnidadMedida.METRO)
                .stockDisponible(new BigDecimal("50"))
                .costeUnitario(new BigDecimal("12.50"))
                .build();
    }

    @Nested
    @DisplayName("eliminar() — RN-04: integridad referencial")
    class Eliminar {

        @Test
        @DisplayName("deberia_lanzarRecursoEnUso_cuandoElMaterialTieneAsignacionesActivas")
        void deberia_lanzarRecursoEnUso_cuandoElMaterialTieneAsignacionesActivas() {
            // Arrange: basta con que la lista no esté vacía; el contenido concreto de la
            // asignación es irrelevante para esta regla, solo importa su existencia.
            Material material = materialDePrueba(5L);
            ProyectoMaterial cualquierAsignacion = ProyectoMaterial.builder().id(1L).build();
            when(materialRepository.findById(5L)).thenReturn(Optional.of(material));
            when(proyectoMaterialRepository.findByMaterialId(5L)).thenReturn(List.of(cualquierAsignacion));

            // Act & Assert
            assertThatThrownBy(() -> materialService.eliminar(5L))
                    .isInstanceOf(RecursoEnUsoException.class)
                    .hasMessageContaining("asignaciones activas");

            verify(materialRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deberia_eliminarElMaterial_cuandoNoTieneAsignacionesActivas")
        void deberia_eliminarElMaterial_cuandoNoTieneAsignacionesActivas() {
            // Arrange
            Material material = materialDePrueba(5L);
            when(materialRepository.findById(5L)).thenReturn(Optional.of(material));
            when(proyectoMaterialRepository.findByMaterialId(5L)).thenReturn(List.of());

            // Act
            materialService.eliminar(5L);

            // Assert
            verify(materialRepository).delete(material);
        }

        @Test
        @DisplayName("deberia_lanzarRecursoNoEncontrado_cuandoElMaterialAEliminarNoExiste")
        void deberia_lanzarRecursoNoEncontrado_cuandoElMaterialAEliminarNoExiste() {
            // Arrange
            when(materialRepository.findById(404L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> materialService.eliminar(404L))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(proyectoMaterialRepository, never()).findByMaterialId(any());
        }
    }

    @Nested
    @DisplayName("obtenerPorId()")
    class ObtenerPorId {

        @Test
        @DisplayName("deberia_devolverElMaterialMapeado_cuandoElIdExiste")
        void deberia_devolverElMaterialMapeado_cuandoElIdExiste() {
            // Arrange
            Material material = materialDePrueba(5L);
            when(materialRepository.findById(5L)).thenReturn(Optional.of(material));

            // Act
            MaterialResponse response = materialService.obtenerPorId(5L);

            // Assert
            assertThat(response.id()).isEqualTo(5L);
            assertThat(response.nombre()).isEqualTo(material.getNombre());
            assertThat(response.stockDisponible()).isEqualByComparingTo(material.getStockDisponible());
        }

        @Test
        @DisplayName("deberia_lanzarRecursoNoEncontrado_cuandoElIdNoExiste")
        void deberia_lanzarRecursoNoEncontrado_cuandoElIdNoExiste() {
            // Arrange
            when(materialRepository.findById(404L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> materialService.obtenerPorId(404L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("404");
        }
    }
}
