package com.delamaderaalcodigo.tallerapi.repository;


import com.delamaderaalcodigo.tallerapi.model.ProyectoMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para {@link ProyectoMaterial}.
 *
 * <p>{@code findByProyectoId} dará soporte en la Fase 2 al endpoint
 * {@code GET /api/v1/proyectos/{id}/materiales}. {@code findByMaterialId}
 * será útil para comprobar la regla RN-4 (no se puede eliminar un
 * Material con asignaciones activas).</p>
 */
@Repository
public interface ProyectoMaterialRepository extends JpaRepository<ProyectoMaterial, Long > {

    List<ProyectoMaterial> findByProyectoId(Long proyectoId);

    List<ProyectoMaterial> findByMaterialId(Long materialId);
}
