package com.delamaderaalcodigo.tallerapi.service;

import com.delamaderaalcodigo.tallerapi.dto.MaterialRequest;
import com.delamaderaalcodigo.tallerapi.dto.MaterialResponse;
import com.delamaderaalcodigo.tallerapi.exception.RecursoEnUsoException;
import com.delamaderaalcodigo.tallerapi.exception.RecursoNoEncontradoException;
import com.delamaderaalcodigo.tallerapi.mapper.MaterialMapper;
import com.delamaderaalcodigo.tallerapi.model.Material;
import com.delamaderaalcodigo.tallerapi.model.TipoMaterial;
import com.delamaderaalcodigo.tallerapi.repository.MaterialRepository;
import com.delamaderaalcodigo.tallerapi.repository.ProyectoMaterialRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;

    private final ProyectoMaterialRepository proyectoMaterialRepository;

    public MaterialService(MaterialRepository materialRepository,
                           ProyectoMaterialRepository proyectoMaterialRepository) {
        this.materialRepository = materialRepository;
        this.proyectoMaterialRepository = proyectoMaterialRepository;
    }

    @Transactional
    public MaterialResponse crear(MaterialRequest request) {
        Material guardado = materialRepository.save(MaterialMapper.aEntidad(request));
        return MaterialMapper.aResponse(guardado);
    }

    public MaterialResponse obtenerPorId(Long id) {
        return MaterialMapper.aResponse(buscarOLanzar(id));
    }

    /**
     * Si {@code tipo} es null, se ignora el filtro y se listan todos
     * los materiales. Decidimos el método de repositorio aquí, en el
     * servicio, en lugar de en el controller, para que la regla
     * "tipo null = sin filtro" sea responsabilidad de la capa de
     * negocio y no de la capa HTTP.
     */
    public Page<MaterialResponse> listar(TipoMaterial tipo, Pageable pageable) {
        Page<Material> pagina = (tipo != null)
                ? materialRepository.findByTipo(tipo, pageable)
                : materialRepository.findAll(pageable);
        return pagina.map(MaterialMapper::aResponse);
    }

    @Transactional
    public MaterialResponse actualizar(Long id, MaterialRequest request) {
        Material material = buscarOLanzar(id);
        MaterialMapper.actualizarEntidad(material, request);
        return MaterialMapper.aResponse(material);
    }

    @Transactional
    public void eliminar(Long id) {
        Material material = buscarOLanzar(id);
        // RN-04: no se puede eliminar un Material con asignaciones activas.
        // A diferencia del caso de Cliente, aquí no hace falta tocar
        // ProyectoMaterialRepository: findByMaterialId ya existía desde
        // antes de esta fase, así que se reutiliza directamente.
        if(!proyectoMaterialRepository.findByMaterialId(id).isEmpty()){
            throw RecursoEnUsoException.paraId("Material", id, "tiene asignaciones activas en proyectos");
        }
        materialRepository.delete(material);
    }

    private Material buscarOLanzar(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.paraId("Material", id));
    }
}