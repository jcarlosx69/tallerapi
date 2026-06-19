package com.delamaderaalcodigo.tallerapi.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de {@link JwtService}.
 *
 * <p>A diferencia de {@code ProyectoServiceTest}, {@code ClienteServiceTest} y
 * {@code MaterialServiceTest}, esta clase NO usa {@code @ExtendWith(MockitoExtension.class)}
 * ni {@code @Mock}: {@code JwtService} no tiene ninguna dependencia de tipo repositorio o
 * servicio que mockear, solo un {@link JwtProperties} que es un simple record de
 * configuración. Se instancia directamente, sin necesidad de levantar contexto de Spring ni
 * de simular nada — ver {@code fase2d1-notas-tecnicas.md} para más detalle sobre por qué esto
 * es justamente lo que hace a esta clase tan fácil de testear de forma aislada.</p>
 */
@DisplayName("JwtService")
class JwtServiceTest {

    // Clave de exactamente 32 bytes (256 bits): el mínimo que exige HS256. Si fuese más
    // corta, el propio constructor de JwtService lanzaría WeakKeyException al arrancar.
    private static final String SECRETO_DE_PRUEBA = "0123456789abcdef0123456789abcdef";

    private JwtService jwtService;

    @BeforeEach
    void crearServicioConExpiracionDeUnaHora() {
        jwtService = new JwtService(new JwtProperties(SECRETO_DE_PRUEBA, 3_600_000L));
    }

    @Nested
    @DisplayName("Token recién generado")
    class TokenRecienGenerado {

        @Test
        @DisplayName("deberia_serValido_cuandoSeAcabaDeGenerar")
        void deberia_serValido_cuandoSeAcabaDeGenerar() {
            // Arrange & Act
            String token = jwtService.generateToken("admin", "ADMIN");

            // Assert
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("deberia_devolverElUsernameYElRolCorrectos_alExtraerlosDelToken")
        void deberia_devolverElUsernameYElRolCorrectos_alExtraerlosDelToken() {
            // Arrange
            String token = jwtService.generateToken("admin", "ADMIN");

            // Act & Assert
            assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
            assertThat(jwtService.extractRol(token)).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("Token con la firma manipulada")
    class TokenConFirmaManipulada {

        @Test
        @DisplayName("deberia_serInvalido_cuandoSeModificaUnCaracterDelPayload")
        void deberia_serInvalido_cuandoSeModificaUnCaracterDelPayload() {
            // Arrange: un JWT compacto tiene tres partes separadas por ".":
            // header.payload.firma. Si se modifica un solo carácter del payload pero se
            // deja la firma original intacta, la firma deja de corresponder al contenido
            // — exactamente lo que ocurriría si alguien interceptase un token y tratase de
            // alterarlo sin conocer el secreto de firma. No hace falta acceder a la clave
            // real para provocar este caso: basta con tocar el texto del propio token.
            String tokenOriginal = jwtService.generateToken("admin", "ADMIN");
            String tokenManipulado = manipularUnCaracterDelPayload(tokenOriginal);

            // Act & Assert
            assertThat(jwtService.isTokenValid(tokenManipulado)).isFalse();
        }

        private String manipularUnCaracterDelPayload(String token) {
            String[] partes = token.split("\\.");
            String payload = partes[1];
            char ultimoCaracter = payload.charAt(payload.length() - 1);
            char caracterSustituto = (ultimoCaracter == 'a') ? 'b' : 'a';
            String payloadManipulado = payload.substring(0, payload.length() - 1) + caracterSustituto;
            return partes[0] + "." + payloadManipulado + "." + partes[2];
        }
    }

    @Nested
    @DisplayName("Token expirado")
    class TokenExpirado {

        @Test
        @DisplayName("deberia_serInvalido_cuandoElTokenYaHaExpirado")
        void deberia_serInvalido_cuandoElTokenYaHaExpirado() {
            // Arrange: en lugar de generar un token válido y esperar con Thread.sleep() a
            // que expire de verdad —lo que haría el test lento y, peor, dependiente del
            // reloj real y propenso a fallos intermitentes en CI—, se construye un
            // JwtService de prueba con una expiración ya transcurrida (un valor negativo
            // de milisegundos). generateToken() sigue produciendo un token con firma
            // perfectamente válida; lo único que cambia es que su claim "exp" queda fijado
            // en un instante anterior al de su propia emisión. Se usa el mismo secreto que
            // el servicio "normal" del @BeforeEach para poder validar el token con él.
            JwtService servicioConExpiracionYaTranscurrida =
                    new JwtService(new JwtProperties(SECRETO_DE_PRUEBA, -1_000L));
            String tokenYaExpirado = servicioConExpiracionYaTranscurrida.generateToken("admin", "ADMIN");

            // Act & Assert
            assertThat(jwtService.isTokenValid(tokenYaExpirado)).isFalse();
        }
    }
}
