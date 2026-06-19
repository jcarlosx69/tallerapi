package com.delamaderaalcodigo.tallerapi.security;

import com.delamaderaalcodigo.tallerapi.security.jwt.JwtAuthenticationFilter;
import com.delamaderaalcodigo.tallerapi.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de Spring Security para TallerAPI.
 *
 * Aquí se decide, en un único sitio, qué rutas son públicas, qué método HTTP requiere qué
 * rol, cómo se gestionan los errores de autenticación/autorización, y en qué orden se
 * ejecuta nuestro filtro JWT respecto a los filtros estándar de Spring Security.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** Rutas accesibles sin token, según el documento de requisitos (login y Swagger). */
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/login",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomAuthenticationEntryPoint authenticationEntryPoint,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protege contra que un sitio externo haga que el NAVEGADOR de un usuario ya
                // autenticado por COOKIE de sesión realice una acción sin que se dé cuenta (el
                // navegador adjunta la cookie automáticamente a cualquier petición, venga de donde
                // venga). Aquí no hay cookies de sesión: el cliente tiene que añadir explícitamente
                // el header "Authorization: Bearer <token>" en cada petición, y un script de otro
                // origen NO puede leer ni adjuntar ese header en nombre del usuario (a diferencia de
                // una cookie). Por eso deshabilitar CSRF en una API stateless basada en JWT no
                // reabre ese vector de ataque; mantenerlo activo aquí solo daría falsos 403 sin
                // aportar protección real.
                .csrf(csrf -> csrf.disable())

                // STATELESS: Spring Security no crea ni consulta HttpSession. Cada request se
                // autentica de forma independiente a partir del JWT que lleva; el servidor no
                // "recuerda" nada de una petición a la siguiente. Esto es lo que permite, el día de
                // mañana, tener varias instancias de la app detrás de un balanceador sin compartir
                // estado de sesión entre ellas.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // Insertamos el filtro JWT justo antes del filtro estándar de usuario/contraseña.
                // Si el JWT ya autenticó la request, ese filtro estándar no tiene nada que hacer
                // (esta API no usa formularios de login).
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exponemos como bean el AuthenticationManager que Spring Boot configura por defecto:
     * al encontrar en el contexto un único UserDetailsService (CustomUserDetailsService) y
     * un PasswordEncoder (BCryptPasswordEncoder, definido arriba), construye automáticamente
     * un DaoAuthenticationProvider con ambos. Lo exponemos explícitamente porque AuthController
     * lo necesita inyectado para llamar a authenticate(...) en el endpoint de login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}