package com.delamaderaalcodigo.tallerapi.controller;

import com.delamaderaalcodigo.tallerapi.dto.ClienteRequest;
import com.delamaderaalcodigo.tallerapi.model.Cliente;
import com.delamaderaalcodigo.tallerapi.support.DatosDePrueba;
import com.delamaderaalcodigo.tallerapi.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;

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
 * Tests de integración de {@code ClienteController}: cadena completa HTTP -&gt; seguridad
 * -&gt; controller -&gt; {@code ClienteService} real -&gt; respuesta serializada.
 * {@code ClienteRepository} y {@code ProyectoRepository} (este último, solo necesario para
 * RN-04 en {@code eliminar()}) se mockean con {@code @MockBean} en {@link IntegrationTestBase}.
 *
 * <p>Los casos de 401/403 ya están cubiertos una vez, de forma genérica, en
 * {@code SeguridadIntegrationTest}; aquí no se repiten — el objetivo de esta clase es
 * confirmar que cada código de negocio (200/201/204/400/404/409) llega correctamente
 * serializado como {@code ErrorResponse} (en los casos de error) con el status HTTP
 * correcto.</p>
 */
@DisplayName("ClienteController")
class ClienteControllerIntegrationTest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/v1/clientes")
    class Listar {

        @Test
        @DisplayName("deberia_devolver200ConElListadoPaginado_cuandoHayClientes")
        void deberia_devolver200ConElListadoPaginado_cuandoHayClientes() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            when(clienteRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(cliente)));

            mockMvc.perform(get("/api/v1/clientes").header("Authorization", tokenAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].nombre").value("Cliente de prueba 1"))
                    .andExpect(jsonPath("$.content[0].email").value("cliente1@correo.com"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clientes/{id}")
    class ObtenerPorId {

        @Test
        @DisplayName("deberia_devolver200ConElCliente_cuandoExiste")
        void deberia_devolver200ConElCliente_cuandoExiste() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

            mockMvc.perform(get("/api/v1/clientes/1").header("Authorization", tokenAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("cliente1@correo.com"));
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoNoExiste")
        void deberia_devolver404_cuandoNoExiste() throws Exception {
            when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/clientes/999").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("No encontrado"))
                    .andExpect(jsonPath("$.message").value("No existe Cliente con id: 999"))
                    .andExpect(jsonPath("$.path").value("/api/v1/clientes/999"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/clientes")
    class Crear {

        @Test
        @DisplayName("deberia_devolver201ConLocation_cuandoLosDatosSonValidos")
        void deberia_devolver201ConLocation_cuandoLosDatosSonValidos() throws Exception {
            // El id no existe hasta que se "guarda": se simula con thenAnswer asignando un
            // id a la misma instancia que llega a save(), igual que haría un
            // GenerationType.IDENTITY real tras el INSERT.
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> {
                Cliente c = invocacion.getArgument(0);
                c.setId(10L);
                return c;
            });

            ClienteRequest request = new ClienteRequest("Carpintería López", "lopez@correo.com", "611222333");

            mockMvc.perform(post("/api/v1/clientes")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/clientes/10"))
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.nombre").value("Carpintería López"));
        }

        @Test
        @DisplayName("deberia_devolver400ConElDetalleDelCampo_cuandoElNombreEstaVacio")
        void deberia_devolver400ConElDetalleDelCampo_cuandoElNombreEstaVacio() throws Exception {
            ClienteRequest request = new ClienteRequest("", null, null);

            mockMvc.perform(post("/api/v1/clientes")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Error de validación"))
                    .andExpect(jsonPath("$.message").value("nombre: El nombre es obligatorio"));

            verify(clienteRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/clientes/{id}")
    class Actualizar {

        @Test
        @DisplayName("deberia_devolver200ConElClienteActualizado_cuandoExiste")
        void deberia_devolver200ConElClienteActualizado_cuandoExiste() throws Exception {
            Cliente existente = DatosDePrueba.cliente(1L);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));

            ClienteRequest request = new ClienteRequest("Nombre actualizado", "nuevo@correo.com", "699000000");

            mockMvc.perform(put("/api/v1/clientes/1")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Nombre actualizado"))
                    .andExpect(jsonPath("$.telefono").value("699000000"));
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoElClienteNoExiste")
        void deberia_devolver404_cuandoElClienteNoExiste() throws Exception {
            when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

            ClienteRequest request = new ClienteRequest("Nombre", "correo@correo.com", null);

            mockMvc.perform(put("/api/v1/clientes/999")
                            .header("Authorization", tokenAdmin())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/clientes/{id}")
    class Eliminar {

        @Test
        @DisplayName("deberia_devolver204_cuandoElClienteNoTieneProyectosAsociados")
        void deberia_devolver204_cuandoElClienteNoTieneProyectosAsociados() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(proyectoRepository.existsByClienteId(1L)).thenReturn(false);

            mockMvc.perform(delete("/api/v1/clientes/1").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNoContent());

            verify(clienteRepository).delete(cliente);
        }

        @Test
        @DisplayName("deberia_devolver404_cuandoElClienteNoExiste")
        void deberia_devolver404_cuandoElClienteNoExiste() throws Exception {
            when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/api/v1/clientes/999").header("Authorization", tokenAdmin()))
                    .andExpect(status().isNotFound());

            verify(proyectoRepository, never()).existsByClienteId(any());
        }

        @Test
        @DisplayName("deberia_devolver409_cuandoElClienteTieneProyectosAsociados_RN04")
        void deberia_devolver409_cuandoElClienteTieneProyectosAsociados_RN04() throws Exception {
            Cliente cliente = DatosDePrueba.cliente(1L);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(proyectoRepository.existsByClienteId(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/v1/clientes/1").header("Authorization", tokenAdmin()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Recurso en uso"))
                    .andExpect(jsonPath("$.message").value("No se puede eliminar Cliente con id: 1 (tiene proyectos asociados)"));

            verify(clienteRepository, never()).delete(any());
        }
    }
}
