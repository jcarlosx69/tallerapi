package com.delamaderaalcodigo.tallerapi.controller;

import com.delamaderaalcodigo.tallerapi.dto.AsignarMaterialRequest;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoRequest;
import com.delamaderaalcodigo.tallerapi.model.Cliente;
import com.delamaderaalcodigo.tallerapi.model.EstadoProyecto;
import com.delamaderaalcodigo.tallerapi.model.Material;
import com.delamaderaalcodigo.tallerapi.model.Proyecto;
import com.delamaderaalcodigo.tallerapi.model.ProyectoMaterial;
import com.delamaderaalcodigo.tallerapi.support.DatosDePrueba;
import com.delamaderaalcodigo.tallerapi.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración de {@code ProyectoController}: el controller más complejo del
 * proyecto, ya que además del CRUD estándar expone la asignación/desasignación de
 * materiales (RN-01/RN-02) y valida transiciones de estado (RN-03) y fechas (RN-06).
 *
 * <p>{@code ProyectoService} depende de los CUATRO repositorios
 * ({@code ProyectoRepository}, {@code ProyectoMaterialRepository},
 * {@code ClienteRepository}, {@code MaterialRepository}), todos ya mockeados en
 * {@link IntegrationTestBase}.</p>
 */
@DisplayName("ProyectoController")
class ProyectoControllerIntegrationTest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/v1/proyectos")
    class Listar {

        @Test
        @DisplayName("deberia_devolver200SinMaterialesEnElListado_cuandoNoSeAplicaNingunFiltro")
        void deberia_devolver200SinMaterialesEnElListado_cuandoNoSeAplicaNingunFiltro() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            Proyecto proyecto = DatosDePrueba.proyecto(10L, EstadoProyecto.EN_CURSO, cliente);
            when(proyectoRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(proyecto)));

            mockMvc.perform(get("/api/v1/proyectos").header("Authorization", tokenAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(10))
                    .andExpect(jsonPath("$.content[0].clienteNombre").value("Cliente de prueba 1"))
                    // El listado paginado no dispara una consulta por fila para cargar
                    // materiales (evita N+1, ver ProyectoMapper.aResponse vs aResponseDetalle):
                    .andExpect(jsonPath("$.content[0].materiales").isEmpty());

            verify(proyectoMaterialRepository, never()).findByProyectoId(any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/proyectos/{id}")
    class ObtenerPorId {

        @Test
        @DisplayName("deberia_devolver200ConLosMaterialesAsignados_cuandoExiste")
        void deberia_devolver200ConLosMaterialesAsignados_cuandoExiste() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            Proyecto proyecto = DatosDePrueba.proyecto(10L, EstadoProyecto.EN_CURSO, cliente);
            Material material = DatosDePrueba.material(5L, new BigDecimal("8"));
            ProyectoMaterial asignacion = DatosDePrueba.asignacion(100L, proyecto, material, new BigDecimal("2"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(proyectoMaterialRepository.findByProyectoId(10L)).thenReturn(List.of(asignacion));

            mockMvc.perform(get("/api/v1/proyectos/10").header("Authorization", tokenAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.materiales[0].materialNombre").value("Tablero de pino"))
                    .andExpect(jsonPath("$.materiales[0].cantidadAsignada").value(2));
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoNoExiste")
        void deberia_devolver404_cuandoNoExiste() throws Exception {
            when(proyectoRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/proyectos/999").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("No existe Proyecto con id: 999"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/proyectos")
    class Crear {

        @Test
        @DisplayName("deberia_devolver201ConEstadoForzadoAEnCurso_ignorandoElEstadoDelRequest")
        void deberia_devolver201ConEstadoForzadoAEnCurso_ignorandoElEstadoDelRequest() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(proyectoRepository.save(any(Proyecto.class))).thenAnswer(invocacion -> {
                Proyecto p = invocacion.getArgument(0);
                p.setId(20L);
                return p;
            });

            // El request pide explícitamente ENTREGADO; el servicio debe ignorarlo.
            ProyectoRequest request = new ProyectoRequest("Armario a medida", 1L,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                    EstadoProyecto.ENTREGADO, new BigDecimal("500.00"));

            mockMvc.perform(post("/api/v1/proyectos")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/proyectos/20"))
                    .andExpect(jsonPath("$.estado").value("EN_CURSO"));
        }

        @Test
        @DisplayName("deberia_devolver400_cuandoLaFechaDeEntregaEsAnteriorALaFechaDeInicio_RN06")
        void deberia_devolver400_cuandoLaFechaDeEntregaEsAnteriorALaFechaDeInicio_RN06() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

            ProyectoRequest request = new ProyectoRequest("Armario a medida", 1L,
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1),
                    EstadoProyecto.EN_CURSO, new BigDecimal("500.00"));

            mockMvc.perform(post("/api/v1/proyectos")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Error de validación"))
                    .andExpect(jsonPath("$.message").value(
                            "La fecha de entrega prevista (2026-01-01) no puede ser anterior a la fecha de inicio (2026-03-01)"));

            verify(proyectoRepository, never()).save(any());
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoElClienteNoExiste")
        void deberia_devolver404_cuandoElClienteNoExiste() throws Exception {
            when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

            ProyectoRequest request = new ProyectoRequest("Armario a medida", 999L,
                    LocalDate.of(2026, 1, 1), null, EstadoProyecto.EN_CURSO, null);

            mockMvc.perform(post("/api/v1/proyectos")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("No existe Cliente con id: 999"));

            verify(proyectoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/proyectos/{id}")
    class Actualizar {

        @Test
        @DisplayName("deberia_devolver200_cuandoLaTransicionDeEstadoEsValida")
        void deberia_devolver200_cuandoLaTransicionDeEstadoEsValida() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            Proyecto existente = DatosDePrueba.proyecto(10L, EstadoProyecto.EN_CURSO, cliente);
            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(existente));
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

            ProyectoRequest request = new ProyectoRequest("Armario a medida", 1L,
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10),
                    EstadoProyecto.TERMINADO, new BigDecimal("850.00"));

            mockMvc.perform(put("/api/v1/proyectos/10")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("TERMINADO"));
        }

        @Test
        @DisplayName("deberia_devolver400_cuandoLaTransicionDeEstadoNoEstaPermitida_RN03")
        void deberia_devolver400_cuandoLaTransicionDeEstadoNoEstaPermitida_RN03() throws Exception {
            // ENTREGADO es un estado terminal: no puede volver a EN_CURSO (caso explícito
            // del documento de requisitos).
            Cliente cliente = DatosDePrueba.cliente(1L);
            Proyecto existente = DatosDePrueba.proyecto(10L, EstadoProyecto.ENTREGADO, cliente);
            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(existente));
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

            ProyectoRequest request = new ProyectoRequest("Armario a medida", 1L,
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10),
                    EstadoProyecto.EN_CURSO, new BigDecimal("850.00"));

            mockMvc.perform(put("/api/v1/proyectos/10")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Transición de estado inválida"))
                    .andExpect(jsonPath("$.message")
                            .value("No se puede pasar del estado ENTREGADO al estado EN_CURSO"));
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoElProyectoNoExiste")
        void deberia_devolver404_cuandoElProyectoNoExiste() throws Exception {
            when(proyectoRepository.findById(999L)).thenReturn(Optional.empty());

            ProyectoRequest request = new ProyectoRequest("Armario a medida", 1L,
                    LocalDate.of(2026, 1, 10), null, EstadoProyecto.TERMINADO, null);

            mockMvc.perform(put("/api/v1/proyectos/999")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            // Ni siquiera debería llegar a comprobar el cliente: buscarOLanzar(id) del
            // propio Proyecto falla antes de eso (ver orden de ProyectoService.actualizar).
            verify(clienteRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/proyectos/{id}")
    class Eliminar {

        @Test
        @DisplayName("deberia_devolver204_cuandoExiste")
        void deberia_devolver204_cuandoExiste() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            Proyecto proyecto = DatosDePrueba.proyecto(10L, EstadoProyecto.EN_CURSO, cliente);
            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(proyectoMaterialRepository.findByProyectoId(10L)).thenReturn(List.of());

            mockMvc.perform(delete("/api/v1/proyectos/10").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNoContent());

            verify(proyectoRepository).delete(proyecto);
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoNoExiste")
        void deberia_devolver404_cuandoNoExiste() throws Exception {
            when(proyectoRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/api/v1/proyectos/999").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/proyectos/{id}/materiales — RN-01")
    class AsignarMaterial {

        @Test
        @DisplayName("deberia_devolver201YDescontarElStock_cuandoHayStockSuficiente")
        void deberia_devolver201YDescontarElStock_cuandoHayStockSuficiente() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            Proyecto proyecto = DatosDePrueba.proyecto(10L, EstadoProyecto.EN_CURSO, cliente);
            Material material = DatosDePrueba.material(5L, new BigDecimal("10"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(materialRepository.findById(5L)).thenReturn(Optional.of(material));
            when(proyectoMaterialRepository.save(any(ProyectoMaterial.class))).thenAnswer(invocacion -> {
                ProyectoMaterial pm = invocacion.getArgument(0);
                pm.setId(200L);
                return pm;
            });

            AsignarMaterialRequest request = new AsignarMaterialRequest(5L, new BigDecimal("4"));

            mockMvc.perform(post("/api/v1/proyectos/10/materiales")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/proyectos/10/materiales"))
                    .andExpect(jsonPath("$.materialId").value(5))
                    .andExpect(jsonPath("$.cantidadAsignada").value(4));

            // Efecto observable de RN-01: el stock se descuenta sobre la MISMA instancia
            // de Material que devolvió el repositorio (no hay un save() explícito sobre
            // Material porque, igual que en ClienteService.actualizar, es una entidad
            // gestionada y el cambio se confía al dirty checking de Hibernate).
            assertThat(material.getStockDisponible()).isEqualByComparingTo("6");
        }

        @Test
        @DisplayName("deberia_devolver409YNoModificarElStock_cuandoElStockEsInsuficiente")
        void deberia_devolver409YNoModificarElStock_cuandoElStockEsInsuficiente() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            Proyecto proyecto = DatosDePrueba.proyecto(10L, EstadoProyecto.EN_CURSO, cliente);
            Material material = DatosDePrueba.material(5L, new BigDecimal("3"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(materialRepository.findById(5L)).thenReturn(Optional.of(material));

            AsignarMaterialRequest request = new AsignarMaterialRequest(5L, new BigDecimal("999"));

            mockMvc.perform(post("/api/v1/proyectos/10/materiales")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Conflicto de stock"));

            assertThat(material.getStockDisponible()).isEqualByComparingTo("3");
            verify(proyectoMaterialRepository, never()).save(any());
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoElMaterialNoExiste")
        void deberia_devolver404_cuandoElMaterialNoExiste() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            Proyecto proyecto = DatosDePrueba.proyecto(10L, EstadoProyecto.EN_CURSO, cliente);
            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(materialRepository.findById(999L)).thenReturn(Optional.empty());

            AsignarMaterialRequest request = new AsignarMaterialRequest(999L, new BigDecimal("1"));

            mockMvc.perform(post("/api/v1/proyectos/10/materiales")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("No existe Material con id: 999"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/proyectos/{id}/materiales")
    class ListarMaterialesAsignados {

        @Test
        @DisplayName("deberia_devolver200ConElListado_cuandoElProyectoExiste")
        void deberia_devolver200ConElListado_cuandoElProyectoExiste() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            Proyecto proyecto = DatosDePrueba.proyecto(10L, EstadoProyecto.EN_CURSO, cliente);
            Material material = DatosDePrueba.material(5L, new BigDecimal("8"));
            ProyectoMaterial asignacion = DatosDePrueba.asignacion(100L, proyecto, material, new BigDecimal("2"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(proyectoMaterialRepository.findByProyectoId(10L)).thenReturn(List.of(asignacion));

            mockMvc.perform(get("/api/v1/proyectos/10/materiales").header("Authorization", tokenAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].materialNombre").value("Tablero de pino"));
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoElProyectoNoExiste")
        void deberia_devolver404_cuandoElProyectoNoExiste() throws Exception {
            when(proyectoRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/proyectos/999/materiales").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/proyectos/{id}/materiales/{materialId} — RN-02")
    class DesasignarMaterial {

        @Test
        @DisplayName("deberia_devolver204YReponerElStock_cuandoLaAsignacionExiste")
        void deberia_devolver204YReponerElStock_cuandoLaAsignacionExiste() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            Proyecto proyecto = DatosDePrueba.proyecto(10L, EstadoProyecto.EN_CURSO, cliente);
            Material material = DatosDePrueba.material(5L, new BigDecimal("6"));
            ProyectoMaterial asignacion = DatosDePrueba.asignacion(100L, proyecto, material, new BigDecimal("4"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(proyectoMaterialRepository.findByProyectoId(10L)).thenReturn(List.of(asignacion));

            mockMvc.perform(delete("/api/v1/proyectos/10/materiales/5").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNoContent());

            assertThat(material.getStockDisponible()).isEqualByComparingTo("10");
            verify(proyectoMaterialRepository).delete(asignacion);
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoElProyectoNoTieneAsignadoEseMaterial")
        void deberia_devolver404_cuandoElProyectoNoTieneAsignadoEseMaterial() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            Proyecto proyecto = DatosDePrueba.proyecto(10L, EstadoProyecto.EN_CURSO, cliente);
            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(proyectoMaterialRepository.findByProyectoId(10L)).thenReturn(List.of());

            mockMvc.perform(delete("/api/v1/proyectos/10/materiales/999").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNotFound());

            verify(proyectoMaterialRepository, never()).delete(any());
        }
    }
}
