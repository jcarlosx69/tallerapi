package com.delamaderaalcodigo.tallerapi.security;

import com.delamaderaalcodigo.tallerapi.dto.ClienteRequest;
import com.delamaderaalcodigo.tallerapi.dto.auth.LoginRequest;
import com.delamaderaalcodigo.tallerapi.model.Rol;
import com.delamaderaalcodigo.tallerapi.model.Usuario;
import com.delamaderaalcodigo.tallerapi.support.DatosDePrueba;
import com.delamaderaalcodigo.tallerapi.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración end-to-end de autenticación y autorización (caso 1 de la
 * especificación de Fase 2d-2): login correcto/incorrecto, acceso sin token
 * ({@code CustomAuthenticationEntryPoint}) y acceso con rol insuficiente
 * ({@code CustomAccessDeniedHandler}).
 *
 * <p>A diferencia de los demás tests de esta fase (que usan {@link #tokenAdmin()} /
 * {@link #tokenUsuario()} para saltarse el login real), los tests de la clase {@link Login}
 * ejercitan {@code POST /api/v1/auth/login} de verdad: es el único sitio donde de verdad
 * importa que la contraseña en claro coincida con el hash BCrypt almacenado, y por tanto el
 * único sitio donde tiene sentido usar {@link DatosDePrueba#usuarioConPassword}.</p>
 */
@DisplayName("Seguridad: autenticación y autorización end-to-end")
class SeguridadIntegrationTest extends IntegrationTestBase {

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("deberia_devolver200ConUnTokenValido_cuandoLasCredencialesSonCorrectas")
        void deberia_devolver200ConUnTokenValido_cuandoLasCredencialesSonCorrectas() throws Exception {
            // Arrange: el hash BCrypt se genera en el momento a partir de "claveSegura123",
            // así que AuthenticationManager.authenticate(...) debe aceptar exactamente esa
            // contraseña en claro.
            Usuario usuario = DatosDePrueba.usuarioConPassword(1L, "carlos", "claveSegura123", Rol.ADMIN);
            when(usuarioRepository.findByUsername("carlos")).thenReturn(Optional.of(usuario));

            LoginRequest request = new LoginRequest("carlos", "claveSegura123");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.expiresAt").isNotEmpty());
        }

        @Test
        @DisplayName("deberia_devolver401_cuandoLaContrasenaEsIncorrecta")
        void deberia_devolver401_cuandoLaContrasenaEsIncorrecta() throws Exception {
            Usuario usuario = DatosDePrueba.usuarioConPassword(1L, "carlos", "claveSegura123", Rol.ADMIN);
            when(usuarioRepository.findByUsername("carlos")).thenReturn(Optional.of(usuario));

            LoginRequest request = new LoginRequest("carlos", "claveIncorrecta");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("No autenticado"))
                    .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
        }

        @Test
        @DisplayName("deberia_devolver401_cuandoElUsuarioNoExiste")
        void deberia_devolver401_cuandoElUsuarioNoExiste() throws Exception {
            // Mismo código de error que una contraseña incorrecta (AuthController captura
            // tanto BadCredentialsException como UsernameNotFoundException y las traduce al
            // mismo mensaje genérico) — es una decisión de seguridad deliberada: no revelar
            // si el problema fue "usuario inexistente" o "contraseña incorrecta".
            when(usuarioRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

            LoginRequest request = new LoginRequest("inexistente", "cualquierClave");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
        }

        @Test
        @DisplayName("deberia_devolver400_cuandoElUsernameVieneVacio")
        void deberia_devolver400_cuandoElUsernameVieneVacio() throws Exception {
            LoginRequest request = new LoginRequest("", "claveSegura123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("username: username es obligatorio"));
        }
    }

    @Nested
    @DisplayName("Acceso sin token (CustomAuthenticationEntryPoint)")
    class SinToken {

        @Test
        @DisplayName("deberia_devolver401_cuandoNoSeEnviaElHeaderAuthorization")
        void deberia_devolver401_cuandoNoSeEnviaElHeaderAuthorization() throws Exception {
            mockMvc.perform(get("/api/v1/clientes"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("No autenticado"))
                    .andExpect(jsonPath("$.message")
                            .value("Se requiere un token JWT válido en el header Authorization (Bearer <token>)"));
        }

        @Test
        @DisplayName("deberia_devolver401_cuandoElTokenEstaManipuladoOEsInvalido")
        void deberia_devolver401_cuandoElTokenEstaManipuladoOEsInvalido() throws Exception {
            mockMvc.perform(get("/api/v1/clientes")
                            .header("Authorization", "Bearer esto-no-es-un-jwt-valido"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("No autenticado"));
        }
    }

    @Nested
    @DisplayName("Acceso con rol insuficiente (CustomAccessDeniedHandler)")
    class RolInsuficiente {

        @Test
        @DisplayName("deberia_devolver403_cuandoUnUsuarioConRolUserIntentaCrearUnCliente")
        void deberia_devolver403_cuandoUnUsuarioConRolUserIntentaCrearUnCliente() throws Exception {
            ClienteRequest request = new ClienteRequest("Cliente nuevo", "nuevo@correo.com", null);

            mockMvc.perform(post("/api/v1/clientes")
                            .header("Authorization", tokenUsuario())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Acceso denegado"))
                    .andExpect(jsonPath("$.message")
                            .value("Tu usuario está autenticado pero no tiene el rol necesario para esta operación"));
        }

        @Test
        @DisplayName("deberia_permitirLaLectura_cuandoElUsuarioTieneRolUser_RN05")
        void deberia_permitirLaLectura_cuandoElUsuarioTieneRolUser_RN05() throws Exception {
            // RN-05: las lecturas (GET) requieren autenticación pero permiten también el rol
            // USER, a diferencia de las escrituras (POST/PUT/DELETE), que exigen ADMIN.
            when(clienteRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(get("/api/v1/clientes").header("Authorization", tokenUsuario()))
                    .andExpect(status().isOk());
        }
    }
}
