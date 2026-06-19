package com.delamaderaalcodigo.tallerapi.support;

import com.delamaderaalcodigo.tallerapi.model.Rol;
import com.delamaderaalcodigo.tallerapi.repository.ClienteRepository;
import com.delamaderaalcodigo.tallerapi.repository.MaterialRepository;
import com.delamaderaalcodigo.tallerapi.repository.ProyectoMaterialRepository;
import com.delamaderaalcodigo.tallerapi.repository.ProyectoRepository;
import com.delamaderaalcodigo.tallerapi.repository.UsuarioRepository;
import com.delamaderaalcodigo.tallerapi.security.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;

/**
 * Clase base para todos los tests de integración de Fase 2d-2.
 *
 * <p><b>Por qué {@code @SpringBootTest(webEnvironment = MOCK) + @AutoConfigureMockMvc} y
 * no {@code @WebMvcTest}:</b> {@code @WebMvcTest} solo carga la capa web (controllers,
 * {@code @ControllerAdvice}, conversores HTTP) y excluye explícitamente la configuración de
 * seguridad salvo que se importe a mano. Necesitamos el {@code SecurityFilterChain} real
 * (con {@code JwtAuthenticationFilter}, {@code CustomAuthenticationEntryPoint},
 * {@code CustomAccessDeniedHandler} ya cableados) para que estos tests verifiquen la cadena
 * completa request -&gt; filtro JWT -&gt; autorización -&gt; controller -&gt; service real
 * -&gt; respuesta serializada. {@code @SpringBootTest} carga el contexto completo de la
 * aplicación, que es justo lo que se necesita aquí — ver
 * {@code fase2d2-notas-tecnicas.md}, sección 1, para la comparación completa con
 * {@code @WebMvcTest} y con los tests unitarios de Fase 2d-1.</p>
 *
 * <p><b>Por qué los CINCO repositorios se mockean aquí, no en cada test class por
 * separado:</b> el contexto de Spring que arranca con {@code @SpringBootTest} es el de la
 * aplicación completa, no solo el controller bajo test. Eso significa que
 * {@code ClienteService}, {@code MaterialService}, {@code ProyectoService} y
 * {@code CustomUserDetailsService} se instancian SIEMPRE, sin importar qué controller se
 * esté probando en una clase concreta — y todos ellos, entre los cuatro, requieren los
 * cinco repositorios del proyecto. Si solo se mockeara el repositorio "obvio" para un
 * controller (p. ej. solo {@code ClienteRepository} en
 * {@code ClienteControllerIntegrationTest}), el arranque del contexto fallaría al intentar
 * inyectar un bean real e inexistente de, por ejemplo, {@code ProyectoMaterialRepository}
 * en {@code MaterialService}.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String USERNAME_ADMIN = "admin-test";
    protected static final String USERNAME_USER = "user-test";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    @MockBean
    protected ClienteRepository clienteRepository;

    @MockBean
    protected MaterialRepository materialRepository;

    @MockBean
    protected ProyectoRepository proyectoRepository;

    @MockBean
    protected ProyectoMaterialRepository proyectoMaterialRepository;

    @MockBean
    protected UsuarioRepository usuarioRepository;

    /**
     * Devuelve un header {@code "Bearer <token>"} válido para un usuario ADMIN.
     *
     * <p><b>Punto clave para entender este método</b> (ver
     * {@code fase2d2-notas-tecnicas.md}, sección 2, para el desarrollo completo):
     * {@code JwtAuthenticationFilter} no se fía del claim {@code "rol"} embebido en el
     * token para decidir la autorización. Tras validar la firma del JWT, vuelve a cargar el
     * {@code Usuario} real desde {@code UsuarioRepository} por su {@code username}
     * ({@code CustomUserDetailsService.loadUserByUsername}) y usa el rol de ESE registro
     * para construir las {@code authorities} de Spring Security. Por eso este método hace
     * dos cosas a la vez: genera el token Y stubea {@code usuarioRepository}, exactamente
     * como si el token se hubiera obtenido pasando por {@code /auth/login} de verdad. Si
     * solo se generara el token sin este stub, la primera petición autenticada lanzaría
     * {@code UsernameNotFoundException} dentro del filtro (no capturada allí) y el test
     * fallaría con un 500 en lugar del código esperado.</p>
     */
    protected String tokenAdmin() {
        return token(USERNAME_ADMIN, Rol.ADMIN);
    }

    /** Igual que {@link #tokenAdmin()} pero con rol USER, para los tests de RN-05/403. */
    protected String tokenUsuario() {
        return token(USERNAME_USER, Rol.USER);
    }

    private String token(String username, Rol rol) {
        when(usuarioRepository.findByUsername(username))
                .thenReturn(Optional.of(DatosDePrueba.usuario(username, rol)));
        return "Bearer " + jwtService.generateToken(username, rol.name());
    }
}
