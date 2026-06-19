package com.delamaderaalcodigo.tallerapi.support;

import com.delamaderaalcodigo.tallerapi.model.Cliente;
import com.delamaderaalcodigo.tallerapi.model.EstadoProyecto;
import com.delamaderaalcodigo.tallerapi.model.Material;
import com.delamaderaalcodigo.tallerapi.model.Proyecto;
import com.delamaderaalcodigo.tallerapi.model.ProyectoMaterial;
import com.delamaderaalcodigo.tallerapi.model.Rol;
import com.delamaderaalcodigo.tallerapi.model.TipoMaterial;
import com.delamaderaalcodigo.tallerapi.model.UnidadMedida;
import com.delamaderaalcodigo.tallerapi.model.Usuario;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Constructores de entidades de prueba, reutilizados por todas las clases de test de
 * integración de Fase 2d-2 (mismo espíritu que los helpers privados de
 * {@code ProyectoServiceTest} en Fase 2d-1, pero compartidos aquí porque varias clases de
 * test distintas — {@code ClienteControllerIntegrationTest},
 * {@code MaterialControllerIntegrationTest}, {@code ProyectoControllerIntegrationTest},
 * {@code SeguridadIntegrationTest} — necesitan las mismas entidades base).
 */
public final class DatosDePrueba {

    /**
     * Instancia propia de {@link BCryptPasswordEncoder}, completamente independiente del
     * bean real de la aplicación (que vive en {@code SecurityConfig}). Se usa solo para
     * generar un hash válido en {@link #usuarioConPassword}; no se inyecta nada de Spring
     * aquí porque esta clase es un simple generador de fixtures, no un componente del
     * contexto.
     */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private DatosDePrueba() {
        // Clase de utilidad: no se instancia.
    }

    public static Cliente cliente(Long id) {
        return Cliente.builder()
                .id(id)
                .nombre("Cliente de prueba " + id)
                .email("cliente" + id + "@correo.com")
                .telefono("600000000")
                .build();
    }

    public static Material material(Long id, BigDecimal stockDisponible) {
        return Material.builder()
                .id(id)
                .nombre("Tablero de pino")
                .tipo(TipoMaterial.MADERA)
                .unidad(UnidadMedida.METRO)
                .stockDisponible(stockDisponible)
                .costeUnitario(new BigDecimal("12.50"))
                .build();
    }

    public static Proyecto proyecto(Long id, EstadoProyecto estado, Cliente cliente) {
        return Proyecto.builder()
                .id(id)
                .nombre("Mesa de comedor a medida")
                .cliente(cliente)
                .fechaInicio(LocalDate.of(2026, 1, 10))
                .fechaEntregaPrevista(LocalDate.of(2026, 2, 10))
                .estado(estado)
                .presupuesto(new BigDecimal("850.00"))
                .build();
    }

    public static ProyectoMaterial asignacion(Long id, Proyecto proyecto, Material material, BigDecimal cantidad) {
        return ProyectoMaterial.builder()
                .id(id)
                .proyecto(proyecto)
                .material(material)
                .cantidadAsignada(cantidad)
                .fechaAsignacion(LocalDateTime.of(2026, 1, 15, 10, 30))
                .build();
    }

    /**
     * Usuario "ligero" para los tests de autorización (RN-05): solo importa su
     * {@code username} y su {@code rol}, porque es lo único que
     * {@code CustomUserDetailsService.loadUserByUsername} necesita para construir las
     * {@code authorities} que decidirán si una petición se autoriza o no. El
     * {@code passwordHash} no se usa nunca en este camino (no se vuelve a comprobar la
     * contraseña en cada petición autenticada por JWT), así que se rellena con un valor
     * cualquiera no vacío, solo para satisfecer la restricción {@code @NotBlank} de la
     * entidad si en algún momento se valida.
     */
    public static Usuario usuario(String username, Rol rol) {
        return Usuario.builder()
                .id(1L)
                .username(username)
                .passwordHash("hash-no-relevante-para-este-test")
                .rol(rol)
                .build();
    }

    /**
     * Usuario con un hash BCrypt REAL de {@code passwordEnClaro}, generado en el momento.
     * Se usa exclusivamente en los tests de {@code POST /api/v1/auth/login}, donde sí se
     * ejecuta la comparación BCrypt real a través de
     * {@code AuthenticationManager.authenticate(...)}. Generarlo en el momento (en lugar de
     * pegar un hash ya calculado como string literal) evita que el test dependa de los
     * detalles internos de una versión concreta de la librería BCrypt.
     */
    public static Usuario usuarioConPassword(Long id, String username, String passwordEnClaro, Rol rol) {
        return Usuario.builder()
                .id(id)
                .username(username)
                .passwordHash(ENCODER.encode(passwordEnClaro))
                .rol(rol)
                .build();
    }
}
