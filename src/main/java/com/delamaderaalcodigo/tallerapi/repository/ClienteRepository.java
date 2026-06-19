package com.delamaderaalcodigo.tallerapi.repository;


import com.delamaderaalcodigo.tallerapi.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para {@link Cliente}
 *
 * <p>Sin lógica adicional en esta fase; {@link JpaRepository} ya
 * proporciona CRUD y paginación ({@code findAll(Pageable)}), que se
 * usará en la Fase 2 para el listado paginado de
 * {@code GET /api/v1/clientes}</p>
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
