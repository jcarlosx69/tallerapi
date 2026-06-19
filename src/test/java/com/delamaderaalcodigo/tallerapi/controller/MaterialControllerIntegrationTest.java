package com.delamaderaalcodigo.tallerapi.controller;

import com.delamaderaalcodigo.tallerapi.dto.MaterialRequest;
import com.delamaderaalcodigo.tallerapi.model.EstadoProyecto;
import com.delamaderaalcodigo.tallerapi.model.Material;
import com.delamaderaalcodigo.tallerapi.model.Proyecto;
import com.delamaderaalcodigo.tallerapi.model.TipoMaterial;
import com.delamaderaalcodigo.tallerapi.model.UnidadMedida;
import com.delamaderaalcodigo.tallerapi.support.DatosDePrueba;
import com.delamaderaalcodigo.tallerapi.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración de {@code MaterialController}. {@code MaterialRepository} y
 * {@code ProyectoMaterialRepository} (este último, solo para RN-04 en {@code eliminar()})
 * se mockean en {@link IntegrationTestBase}.
 */
@DisplayName("MaterialController")
class MaterialControllerIntegrationTest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/v1/materiales")
    class Listar {

        @Test
        @DisplayName("deberia_llamarAFindAll_cuandoNoSeEnviaElParametroTipo")
        void deberia_llamarAFindAll_cuandoNoSeEnviaElParametroTipo() throws Exception {
            when(materialRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(DatosDePrueba.material(1L, new BigDecimal("10")))));

            mockMvc.perform(get("/api/v1/materiales").header("Authorization", tokenAdmin()))
                    .andExpect(status().isOk());

            verify(materialRepository).findAll(any(Pageable.class));
            verify(materialRepository, never()).findByTipo(any(), any());
        }

        @Test
        @DisplayName("deberia_llamarAFindByTipo_cuandoSeEnviaElParametroTipo")
        void deberia_llamarAFindByTipo_cuandoSeEnviaElParametroTipo() throws Exception {
            when(materialRepository.findByTipo(eq(TipoMaterial.MADERA), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(DatosDePrueba.material(1L, new BigDecimal("10")))));

            mockMvc.perform(get("/api/v1/materiales")
                            .param("tipo", "MADERA")
                            .header("Authorization", tokenAdmin()))
                    .andExpect(status().isOk());

            verify(materialRepository).findByTipo(eq(TipoMaterial.MADERA), any(Pageable.class));
            verify(materialRepository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/materiales/{id}")
    class ObtenerPorId {

        @Test
        @DisplayName("deberia_devolver200ConElFormatoExactoDeMaterialResponse_cuandoExiste")
        void deberia_devolver200ConElFormatoExactoDeMaterialResponse_cuandoExiste() throws Exception {
            // Construido a mano (no con DatosDePrueba.material) para controlar la escala
            // exacta de los BigDecimal y comprobar que el JSON de salida conserva esa
            // escala (12.500 con tres decimales, 9.90 con dos), no solo el valor numérico.
            Material material = Material.builder()
                    .id(1L)
                    .nombre("Tablero de pino")
                    .tipo(TipoMaterial.MADERA)
                    .unidad(UnidadMedida.METRO)
                    .stockDisponible(new BigDecimal("12.500"))
                    .costeUnitario(new BigDecimal("9.90"))
                    .build();
            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

            mockMvc.perform(get("/api/v1/materiales/1").header("Authorization", tokenAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Tablero de pino"))
                    .andExpect(jsonPath("$.tipo").value("MADERA"))
                    .andExpect(jsonPath("$.unidad").value("METRO"))
                    .andExpect(content().string(containsString("\"stockDisponible\":12.500")))
                    .andExpect(content().string(containsString("\"costeUnitario\":9.90")));
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoNoExiste")
        void deberia_devolver404_cuandoNoExiste() throws Exception {
            when(materialRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/materiales/999").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("No existe Material con id: 999"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/materiales")
    class Crear {

        @Test
        @DisplayName("deberia_devolver201_cuandoLosDatosSonValidos")
        void deberia_devolver201_cuandoLosDatosSonValidos() throws Exception {
            when(materialRepository.save(any(Material.class))).thenAnswer(invocacion -> {
                Material m = invocacion.getArgument(0);
                m.setId(7L);
                return m;
            });

            MaterialRequest request = new MaterialRequest("Barniz incoloro", TipoMaterial.BARNIZ,
                    UnidadMedida.LITRO, new BigDecimal("5"), new BigDecimal("18.00"));

            mockMvc.perform(post("/api/v1/materiales")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/materiales/7"))
                    .andExpect(jsonPath("$.nombre").value("Barniz incoloro"));
        }

        @Test
        @DisplayName("deberia_devolver400_cuandoElCosteUnitarioEsNegativo")
        void deberia_devolver400_cuandoElCosteUnitarioEsNegativo() throws Exception {
            MaterialRequest request = new MaterialRequest("Barniz incoloro", TipoMaterial.BARNIZ,
                    UnidadMedida.LITRO, new BigDecimal("5"), new BigDecimal("-1"));

            mockMvc.perform(post("/api/v1/materiales")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("costeUnitario: El coste unitario no puede ser negativo"));

            verify(materialRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/materiales/{id}")
    class Actualizar {

        @Test
        @DisplayName("deberia_devolver200_cuandoExiste")
        void deberia_devolver200_cuandoExiste() throws Exception {
            Material existente = DatosDePrueba.material(1L, new BigDecimal("10"));
            when(materialRepository.findById(1L)).thenReturn(Optional.of(existente));

            MaterialRequest request = new MaterialRequest("Tablero de pino actualizado", TipoMaterial.MADERA,
                    UnidadMedida.METRO, new BigDecimal("20"), new BigDecimal("13.00"));

            mockMvc.perform(put("/api/v1/materiales/1")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Tablero de pino actualizado"));
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoNoExiste")
        void deberia_devolver404_cuandoNoExiste() throws Exception {
            when(materialRepository.findById(999L)).thenReturn(Optional.empty());

            MaterialRequest request = new MaterialRequest("X", TipoMaterial.OTRO,
                    UnidadMedida.UNIDAD, new BigDecimal("1"), new BigDecimal("1"));

            mockMvc.perform(put("/api/v1/materiales/999")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/materiales/{id}")
    class Eliminar {

        @Test
        @DisplayName("deberia_devolver204_cuandoNoTieneAsignacionesActivas")
        void deberia_devolver204_cuandoNoTieneAsignacionesActivas() throws Exception {
            Material material = DatosDePrueba.material(1L, new BigDecimal("10"));
            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
            when(proyectoMaterialRepository.findByMaterialId(1L)).thenReturn(List.of());

            mockMvc.perform(delete("/api/v1/materiales/1").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNoContent());

            verify(materialRepository).delete(material);
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoNoExiste")
        void deberia_devolver404_cuandoNoExiste() throws Exception {
            when(materialRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/api/v1/materiales/999").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deberia_devolver409_cuandoTieneAsignacionesActivas_RN04")
        void deberia_devolver409_cuandoTieneAsignacionesActivas_RN04() throws Exception {
            Material material = DatosDePrueba.material(1L, new BigDecimal("10"));
            Proyecto proyecto = DatosDePrueba.proyecto(1L, EstadoProyecto.EN_CURSO, DatosDePrueba.cliente(1L));
            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
            when(proyectoMaterialRepository.findByMaterialId(1L))
                    .thenReturn(List.of(DatosDePrueba.asignacion(100L, proyecto, material, new BigDecimal("2"))));

            mockMvc.perform(delete("/api/v1/materiales/1").header("Authorization", tokenAdmin()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Recurso en uso"));

            verify(materialRepository, never()).delete(any());
        }
    }
}
