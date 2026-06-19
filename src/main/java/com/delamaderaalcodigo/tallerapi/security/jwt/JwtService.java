package com.delamaderaalcodigo.tallerapi.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Responsabilidad única: generar, validar y leer el contenido de un JWT.
 *
 * Deliberadamente esta clase NO conoce Spring Security (no sabe qué es un SecurityContext
 * ni un UserDetails). Eso la hace testeable de forma totalmente aislada (un test unitario
 * puede generar un token y comprobar que isTokenValid(...) devuelve true/false sin levantar
 * ningún contexto de seguridad) y reutilizable si el día de mañana cambia el mecanismo de
 * autenticación.
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Nombre del claim personalizado donde guardamos el rol (ADMIN/USER). */
    public static final String CLAIM_ROL = "rol";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(JwtProperties jwtProperties) {
        // Keys.hmacShaKeyFor elige el algoritmo HMAC según la longitud de la clave en bytes:
        // 32 bytes -> HS256, 48 -> HS384, 64 -> HS512. Como el requisito es HS256, el secreto
        // configurado en JWT_SECRET debe tener exactamente (o al menos) 32 bytes; si es más
        // corto, este constructor lanza WeakKeyException AL ARRANCAR la aplicación, que es
        // mucho mejor que descubrirlo en producción con tokens que nadie puede validar.
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = jwtProperties.expirationMs();
    }

    /**
     * Genera un JWT firmado para el usuario dado.
     *
     * @param username se usa como "subject" (claim estándar "sub")
     * @param rol      claim personalizado, p.ej. "ADMIN" o "USER" (sin el prefijo ROLE_)
     */
    public String generateToken(String username, String rol) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROL, rol)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey) // con una SecretKey de 32 bytes, JJWT infiere HS256
                .compact();
    }

    public Instant extractExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRol(String token) {
        return parseClaims(token).get(CLAIM_ROL, String.class);
    }

    /**
     * Valida firma y expiración. Devuelve un booleano en lugar de propagar la excepción:
     * el filtro que llama a este método solo necesita un sí/no para decidir si autentica
     * la petición o la deja pasar como anónima (las reglas de autorización ya se encargarán
     * de rechazarla si la ruta lo requiere).
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT caducado: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Firma de JWT inválida: token manipulado o secreto incorrecto");
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT no válido: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        // verifyWith(signingKey) hace que la verificación de firma ocurra DENTRO del parseo,
        // antes de devolver cualquier claim. Si la firma no coincide con el contenido del
        // token, parseSignedClaims lanza SignatureException y getPayload() nunca se ejecuta:
        // por eso es imposible leer un payload manipulado sin conocer el secreto.
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}