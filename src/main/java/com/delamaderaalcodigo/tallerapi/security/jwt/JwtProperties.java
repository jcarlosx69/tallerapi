package com.delamaderaalcodigo.tallerapi.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mapea las propiedades app.jwt.* de application.yml a un objeto fuertemente tipado.
 *
 * Se usa @ConfigurationProperties (en lugar de varios @Value sueltos) por dos motivos:
 *
 *  1. Cohesión: todo lo relacionado con el JWT vive en un único sitio, en vez de tener
 *     "app.jwt.secret" y "app.jwt.expiration-ms" repartidos como @Value en varias clases.
 *  2. Testabilidad: en un test unitario de JwtService basta con hacer
 *     `new JwtProperties("secreto-de-prueba-32-bytes-min!", 3600000L)`
 *     sin necesidad de levantar el contexto de Spring ni mockear @Value.
 *
 * Al ser un record, Spring Boot 3 hace constructor-binding automáticamente: no hace falta
 * @ConstructorBinding explícito salvo que la clase tenga varios constructores.
 *
 * El binding es "relaxed": la propiedad "expiration-ms" (kebab-case en YAML) se mapea sola
 * al componente "expirationMs" (camelCase en Java).
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {
}