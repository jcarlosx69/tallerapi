package com.delamaderaalcodigo.tallerapi.repository;


import com.delamaderaalcodigo.tallerapi.model.Material;
import com.delamaderaalcodigo.tallerapi.model.TipoMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repositorio JPA para {@link Material}.
 *
 * <p>Se incluye {@code findByTipo} como mètodo derivado: el documento
 * der requisitos especifica que {@code GET /api/v1/materiales} admite
 * filtro por {@code tipo}, y Spring Data permite expresar esto sin
 * escribir SQL ni JPQL manual, simplemente nombrando el método según convención.</p>
 */
@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    Page<Material> findByTipo(TipoMaterial tipo, Pageable pageable);
}
