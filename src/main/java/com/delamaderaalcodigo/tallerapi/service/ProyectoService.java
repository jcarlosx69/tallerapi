package com.delamaderaalcodigo.tallerapi.service;

import com.delamaderaalcodigo.tallerapi.dto.AsignarMaterialRequest;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoMaterialResponse;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoRequest;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoResponse;
import com.delamaderaalcodigo.tallerapi.exception.FechaEntregaInvalidaException;
import com.delamaderaalcodigo.tallerapi.exception.RecursoNoEncontradoException;
import com.delamaderaalcodigo.tallerapi.exception.StockInsuficienteException;
import com.delamaderaalcodigo.tallerapi.exception.TransicionEstadoInvalidaException;
import com.delamaderaalcodigo.tallerapi.mapper.ProyectoMapper;
import com.delamaderaalcodigo.tallerapi.mapper.ProyectoMaterialMapper;
import com.delamaderaalcodigo.tallerapi.model.Cliente;
import com.delamaderaalcodigo.tallerapi.model.EstadoProyecto;
import com.delamaderaalcodigo.tallerapi.model.Material;
import com.delamaderaalcodigo.tallerapi.model.Proyecto;
import com.delamaderaalcodigo.tallerapi.model.ProyectoMaterial;
import com.delamaderaalcodigo.tallerapi.repository.ClienteRepository;
import com.delamaderaalcodigo.tallerapi.repository.MaterialRepository;
import com.delamaderaalcodigo.tallerapi.repository.ProyectoMaterialRepository;
import com.delamaderaalcodigo.tallerapi.repository.ProyectoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IMPORTANTE — acción manual requerida en {@code ProyectoRepository}:
 * este servicio necesita dos métodos derivados nuevos. Añade
 * manualmente estas dos firmas a la interfaz:
 *
 * <pre>{@code
 * boolean existsByClienteId(Long clienteId);
 *
 * Page<Proyecto> findByEstadoAndFechaInicioBetween(
 *         EstadoProyecto estado, LocalDate fechaDesde, LocalDate fechaHasta, Pageable pageable);
 * }</pre>
 * <p>
 * Ver fase2c-notas-tecnicas.md, sección 6, para el porqué de cada una.
 */
@Service
public class ProyectoService {

    /**
     * RN-03: transiciones válidas desde cada estado. ENTREGADO y
     * CANCELADO son estados terminales (conjunto vacío de destinos).
     * Ver fase2c-notas-tecnicas.md, sección 3, para la justificación
     * completa, incluyendo por qué CANCELADO solo es alcanzable desde
     * EN_CURSO y TERMINADO, y no desde ENTREGADO.
     */
    private static final Map<EstadoProyecto, Set<EstadoProyecto>> TRANSICIONES_VALIDAS = Map.of(
            EstadoProyecto.EN_CURSO, Set.of(EstadoProyecto.TERMINADO, EstadoProyecto.CANCELADO),
            EstadoProyecto.TERMINADO, Set.of(EstadoProyecto.ENTREGADO, EstadoProyecto.CANCELADO),
            EstadoProyecto.ENTREGADO, Set.of(),
            EstadoProyecto.CANCELADO, Set.of()
    );

    private final ProyectoRepository proyectoRepository;
    private final ProyectoMaterialRepository proyectoMaterialRepository;
    private final ClienteRepository clienteRepository;
    private final MaterialRepository materialRepository;

    public ProyectoService(ProyectoRepository proyectoRepository,
                           ProyectoMaterialRepository proyectoMaterialRepository,
                           ClienteRepository clienteRepository,
                           MaterialRepository materialRepository) {
        this.proyectoRepository = proyectoRepository;
        this.proyectoMaterialRepository = proyectoMaterialRepository;
        this.clienteRepository = clienteRepository;
        this.materialRepository = materialRepository;
    }

    @Transactional
    public ProyectoResponse crear(ProyectoRequest request) {
        Cliente cliente = buscarClienteOLanzar(request.clienteId());
        validarFechas(request.fechaInicio(), request.fechaEntregaPrevista());

        Proyecto proyecto = ProyectoMapper.aEntidad(request, cliente);
        // Todo proyecto nace EN_CURSO, independientemente de lo que llegue
        // en request.estado(): ver javadoc de ProyectoRequest y
        // fase2c-notas-tecnicas.md, sección 7.
        proyecto.setEstado(EstadoProyecto.EN_CURSO);

        Proyecto guardado = proyectoRepository.save(proyecto);
        return ProyectoMapper.aResponse(guardado);
    }

    /**
     * Lectura que navega {@code Proyecto.cliente} (relación
     * {@code @ManyToOne(LAZY)}) para construir {@code clienteNombre} en
     * el DTO. Sin {@code @Transactional(readOnly = true)}, esa
     * navegación fallaría con {@code LazyInitializationException} en
     * cuanto la sesión de Hibernate que abrió la petición se cerrara
     * antes de que el mapper accediera a {@code cliente.getNombre()}.
     * Ver fase2c-notas-tecnicas.md, sección 1.
     */
    @Transactional(readOnly = true)
    public Page<ProyectoResponse> listar(EstadoProyecto estado, LocalDate fechaDesde, LocalDate fechaHasta,
                                         Pageable pageable) {
        boolean filtrarPorFechas = fechaDesde != null && fechaHasta != null;
        // Si solo se envía uno de los dos parámetros de fecha, se ignora el
        // filtro de fechas (no se lanza 400): se documenta esta decisión en
        // fase2c-notas-tecnicas.md, sección 7, en lugar de forzar al cliente
        // de la API a enviar siempre el par completo o ninguno.
        Page<Proyecto> pagina;
        if (estado != null && filtrarPorFechas) {
            pagina = proyectoRepository.findByEstadoAndFechaInicioBetween(estado, fechaDesde, fechaHasta, pageable);
        } else if (estado != null) {
            pagina = proyectoRepository.findByEstado(estado, pageable);
        } else if (filtrarPorFechas) {
            pagina = proyectoRepository.findByFechaInicioBetween(fechaDesde, fechaHasta, pageable);
        } else {
            pagina = proyectoRepository.findAll(pageable);
        }
        return pagina.map(ProyectoMapper::aResponse);
    }

    /**
     * Detalle de un proyecto, incluyendo sus materiales asignados. Aquí
     * hay dos motivos para {@code @Transactional(readOnly = true)}: la
     * navegación lazy de {@code Proyecto.cliente} (igual que en
     * {@code listar}) y la consulta explícita a
     * {@code ProyectoMaterialRepository.findByProyectoId}, que a su vez
     * navega {@code ProyectoMaterial.material} para construir
     * {@code materialNombre}/{@code unidad} en cada
     * {@code ProyectoMaterialResponse}.
     */
    @Transactional(readOnly = true)
    public ProyectoResponse obtenerPorId(Long id) {
        Proyecto proyecto = buscarOLanzar(id);
        List<ProyectoMaterial> materiales = proyectoMaterialRepository.findByProyectoId(id);
        return ProyectoMapper.aResponseDetalle(proyecto, materiales);
    }

    @Transactional
    public ProyectoResponse actualizar(Long id, ProyectoRequest request) {
        Proyecto proyecto = buscarOLanzar(id);
        Cliente cliente = buscarClienteOLanzar(request.clienteId());
        validarFechas(request.fechaInicio(), request.fechaEntregaPrevista());
        validarTransicionEstado(proyecto.getEstado(), request.estado());

        ProyectoMapper.actualizarEntidad(proyecto, request, cliente);
        // Sin save() explícito: "proyecto" es una entidad gestionada dentro
        // de esta transacción, igual que en ClienteService.actualizar().
        return ProyectoMapper.aResponse(proyecto);
    }

    @Transactional
    public void eliminar(Long id) {
        Proyecto proyecto = buscarOLanzar(id);

        // No está pedido explícitamente por ninguna RN, pero se aplica el
        // mismo principio que RN-02: si se elimina el proyecto completo, sus
        // asignaciones de material deben reponer stock antes de
        // desaparecer, o ese stock quedaría "perdido" de forma silenciosa.
        // Ver fase2c-notas-tecnicas.md, sección 8.
        List<ProyectoMaterial> asignaciones = proyectoMaterialRepository.findByProyectoId(id);
        for (ProyectoMaterial asignacion : asignaciones) {
            reponerStock(asignacion);
            proyectoMaterialRepository.delete(asignacion);
        }

        proyectoRepository.delete(proyecto);
    }

    /**
     * RN-01: asigna un material a un proyecto, validando stock
     * disponible y descontándolo de forma atómica (ver
     * fase2c-notas-tecnicas.md, sección 2, para el detalle de
     * atomicidad).
     */
    @Transactional
    public ProyectoMaterialResponse asignarMaterial(Long proyectoId, AsignarMaterialRequest request) {
        Proyecto proyecto = buscarOLanzar(proyectoId);
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> RecursoNoEncontradoException.paraId("Material", request.materialId()));

        if (request.cantidadAsignada().compareTo(material.getStockDisponible()) > 0) {
            throw StockInsuficienteException.de(material.getNombre(), request.cantidadAsignada(),
                    material.getStockDisponible());
        }

        material.setStockDisponible(material.getStockDisponible().subtract(request.cantidadAsignada()));
        // Sin materialRepository.save(): "material" viene de findById(), es una
        // entidad gestionada en esta transacción; Hibernate aplica el cambio
        // por dirty checking al hacer commit (mismo patrón que en Fase 2b).

        ProyectoMaterial asignacion = ProyectoMaterial.builder()
                .proyecto(proyecto)
                .material(material)
                .cantidadAsignada(request.cantidadAsignada())
                // CORRECCIÓN DE REVISIÓN: ProyectoMaterial.fechaAsignacion se
                // autogenera a nivel de BASE DE DATOS (DEFAULT now() en
                // V1__create_schema.sql), no mediante @PrePersist en la
                // entidad. Un INSERT con esta columna explícitamente en NULL
                // NO activa ese DEFAULT y violaría el NOT NULL. Por eso se
                // fija aquí, en la capa de aplicación.
                .fechaAsignacion(LocalDateTime.now())
                .build();

        ProyectoMaterial guardada = proyectoMaterialRepository.save(asignacion);
        return ProyectoMaterialMapper.aResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<ProyectoMaterialResponse> listarMaterialesAsignados(Long proyectoId) {
        buscarOLanzar(proyectoId); // 404 si el proyecto no existe, antes de listar nada
        return proyectoMaterialRepository.findByProyectoId(proyectoId).stream()
                .map(ProyectoMaterialMapper::aResponse)
                .toList();
    }

    /**
     * RN-02: revierte una asignación de material, reponiendo stock de
     * forma atómica junto con el borrado de la fila de
     * {@code ProyectoMaterial}.
     */
    @Transactional
    public void desasignarMaterial(Long proyectoId, Long materialId) {
        buscarOLanzar(proyectoId);

        // Se reutiliza findByProyectoId (ya existente) y se filtra en memoria
        // por materialId, en lugar de añadir un tercer método derivado
        // (findByProyectoIdAndMaterialId) a ProyectoMaterialRepository. Ver
        // fase2c-notas-tecnicas.md, sección 6.
        ProyectoMaterial asignacion = proyectoMaterialRepository.findByProyectoId(proyectoId).stream()
                .filter(pm -> pm.getMaterial().getId().equals(materialId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El proyecto con id: %d no tiene asignado el material con id: %d"
                                .formatted(proyectoId, materialId)));

        reponerStock(asignacion);
        proyectoMaterialRepository.delete(asignacion);
    }

    private void reponerStock(ProyectoMaterial asignacion) {
        Material material = asignacion.getMaterial();
        material.setStockDisponible(material.getStockDisponible().add(asignacion.getCantidadAsignada()));
    }

    private void validarFechas(LocalDate fechaInicio, LocalDate fechaEntregaPrevista) {
        if (fechaEntregaPrevista != null && fechaEntregaPrevista.isBefore(fechaInicio)) {
            throw new FechaEntregaInvalidaException(
                    "La fecha de entrega prevista (%s) no puede ser anterior a la fecha de inicio (%s)"
                            .formatted(fechaEntregaPrevista, fechaInicio));
        }
    }

    private void validarTransicionEstado(EstadoProyecto actual, EstadoProyecto destino) {
        if (actual == destino) {
            return; // Sin cambio de estado: operación idempotente, no es una transición.
        }
        if (!TRANSICIONES_VALIDAS.get(actual).contains(destino)) {
            throw TransicionEstadoInvalidaException.de(actual, destino);
        }
    }

    private Cliente buscarClienteOLanzar(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> RecursoNoEncontradoException.paraId("Cliente", clienteId));
    }

    private Proyecto buscarOLanzar(Long id) {
        return proyectoRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.paraId("Proyecto", id));
    }
}