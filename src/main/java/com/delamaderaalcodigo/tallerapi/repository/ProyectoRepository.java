package com.delamaderaalcodigo.tallerapi.repository;


import com.delamaderaalcodigo.tallerapi.model.EstadoProyecto;
import com.delamaderaalcodigo.tallerapi.model.Proyecto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Repositorio JPA para {@link Proyecto}.
 *
 * <p>Se añaden dos métodos derivados pensando en los filtros que pide
 * el documento de requisitos para {@code GET /api/v1/proyectos}
 * (filtro por estado y por rango de fechas). La lógica de combinar
 * ambos filtros de forma dinámica(por ejemplo, con
 * {@code Specification} o un método de servicio que decida qué
 * consulta llamar) se desarrollará en la Fase 2; aquí solo se deja
 * preparada la capa de acceso a datos.</p>
 */
@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    Page<Proyecto> findByEstado(EstadoProyecto estado, Pageable pageable);

    Page<Proyecto> findByFechaInicioBetween(LocalDate desde, LocalDate hasta, Pageable pageable);

    boolean existsByClienteId(Long clienteId);

    Page<Proyecto> findByEstadoAndFechaInicioBetween(
            EstadoProyecto estado, LocalDate fechaDesde, LocalDate fechaHasta, Pageable pageable
    );
}
