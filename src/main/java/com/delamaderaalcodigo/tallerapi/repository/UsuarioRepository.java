package com.delamaderaalcodigo.tallerapi.repository;


import com.delamaderaalcodigo.tallerapi.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para {@link Usuario}.
 *
 * <p>{@code findByUsername} es el método derivado clave para la
 * autenticación: Spring Security necesita buscar un usuario por su
 * nombre para validad credenciales en el login (Fase 2). se devuelve
 * {@link Optional} para evitar {@code null} y forzar al llamador a
 * manejar explícitamente el caso "usuario no encontrado".</p>
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);
}
