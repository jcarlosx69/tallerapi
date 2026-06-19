package com.delamaderaalcodigo.tallerapi.service;

import com.delamaderaalcodigo.tallerapi.dto.ClienteRequest;
import com.delamaderaalcodigo.tallerapi.dto.ClienteResponse;
import com.delamaderaalcodigo.tallerapi.exception.RecursoEnUsoException;
import com.delamaderaalcodigo.tallerapi.exception.RecursoNoEncontradoException;
import com.delamaderaalcodigo.tallerapi.mapper.ClienteMapper;
import com.delamaderaalcodigo.tallerapi.model.Cliente;
import com.delamaderaalcodigo.tallerapi.repository.ClienteRepository;
import com.delamaderaalcodigo.tallerapi.repository.ProyectoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    private final ProyectoRepository proyectoRepository;

    public ClienteService(ClienteRepository clienteRepository, ProyectoRepository proyectoRepository) {
        this.clienteRepository = clienteRepository;
        this.proyectoRepository = proyectoRepository;
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        Cliente guardado = clienteRepository.save(ClienteMapper.aEntidad(request));
        return ClienteMapper.aResponse(guardado);
    }

    public ClienteResponse obtenerPorId(Long id) {
        return ClienteMapper.aResponse(buscarOLanzar(id));
    }

    public Page<ClienteResponse> listar(Pageable pageable) {
        return clienteRepository.findAll(pageable).map(ClienteMapper::aResponse);
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarOLanzar(id);
        ClienteMapper.actualizarEntidad(cliente, request);
        // No se llama a save(): "cliente" es una entidad gestionada (managed) dentro
        // de esta transacción. Hibernate detecta los cambios (dirty checking) y
        // genera el UPDATE al hacer commit, al salir del método @Transactional.
        return ClienteMapper.aResponse(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscarOLanzar(id);
        // RN-04: no se puede eliminar un Cliente con Proyectos asociados.
        // Requiere añadir manualmente "boolean existsByClienteId(Long
        // clienteId);" a ProyectoRepository — ver el aviso al inicio de
        // ProyectoService y fase2c-notas-tecnicas.md, sección 6.
        if(proyectoRepository.existsByClienteId(id)){
            throw RecursoEnUsoException.paraId("Cliente", id, "tiene proyectos asociados");
        }
        clienteRepository.delete(cliente);
    }

    private Cliente buscarOLanzar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.paraId("Cliente", id));
    }
}