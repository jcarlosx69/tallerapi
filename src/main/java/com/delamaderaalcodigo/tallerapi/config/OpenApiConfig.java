package com.delamaderaalcodigo.tallerapi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configura la documentación OpenAPI/Swagger para TallerAPI.
 *
 * @SecurityScheme declara que la API usa autenticación Bearer JWT:
 * esto hace aparecer el botón "Authorize" en Swagger UI, donde se
 * puede pegar el token obtenido en POST /api/v1/auth/login para
 * que Swagger incluya automáticamente el header
 * "Authorization: Bearer <token>" en todas las peticiones de prueba.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TallerAPI",
                version = "1.0",
                description = "API REST para gestión de inventario y proyectos de un taller de carpintería"
        ),
        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
)

@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Introduce el token JWT obtenido en POST /api/v1/auth/login (sin escribir 'Bearer', Swagger lo añade solo)"
)
public class OpenApiConfig {
}