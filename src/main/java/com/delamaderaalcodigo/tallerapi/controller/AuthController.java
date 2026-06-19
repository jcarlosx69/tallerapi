package com.delamaderaalcodigo.tallerapi.controller;

import com.delamaderaalcodigo.tallerapi.dto.ErrorResponse;
import com.delamaderaalcodigo.tallerapi.dto.auth.LoginRequest;
import com.delamaderaalcodigo.tallerapi.dto.auth.LoginResponse;
import com.delamaderaalcodigo.tallerapi.security.jwt.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Login y comprobación de identidad")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica un usuario y devuelve un JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login correcto, devuelve el token"),
            @ApiResponse(responseCode = "400", description = "Petición mal formada (username/password vacíos)"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            // authenticate() dispara internamente:
            //   1. CustomUserDetailsService.loadUserByUsername(username)
            //   2. PasswordEncoder.matches(rawPassword, passwordHash)
            // Por seguridad, NO distinguimos en la respuesta si el fallo es "usuario
            // inexistente" o "contraseña incorrecta" — un mensaje genérico evita dar pistas
            // a quien intenta adivinar usuarios válidos por fuerza bruta.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            String rol = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .map(auth -> auth.replace("ROLE_", ""))
                    .orElseThrow();

            String token = jwtService.generateToken(authentication.getName(), rol);
            Instant expiresAt = jwtService.extractExpiration(token);

            return ResponseEntity.ok(new LoginResponse(token, expiresAt));

        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            // Normalizamos ambos casos a una única excepción con mensaje genérico, capturada
            // más abajo por el @ExceptionHandler de esta misma clase.
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }
    }

    /**
     * Endpoint de prueba pensado solo para esta fase: confirma de forma sencilla que el
     * filtro JWT está dejando un usuario autenticado en el SecurityContext. En fases
     * posteriores los controllers de Proyecto/Material/Cliente no necesitan un endpoint
     * como este — esto es solo para verificar Fase 2a de forma aislada.
     */
    @GetMapping("/me")
    @Operation(summary = "Devuelve el usuario autenticado actual (endpoint de prueba de esta fase)")
    public ResponseEntity<String> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok("Autenticado como: " + username);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                              HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                401,
                "No autenticado",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(401).body(body);
    }
}