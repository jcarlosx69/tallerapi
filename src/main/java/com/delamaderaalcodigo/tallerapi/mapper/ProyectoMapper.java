package com.delamaderaalcodigo.tallerapi.mapper;


import com.delamaderaalcodigo.tallerapi.dto.ProyectoMaterialResponse;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoRequest;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoResponse;
import com.delamaderaalcodigo.tallerapi.model.Cliente;
import com.delamaderaalcodigo.tallerapi.model.Proyecto;
import com.delamaderaalcodigo.tallerapi.model.ProyectoMaterial;

import java.util.Collections;
import java.util.List;

/**
 * Conversión manual entidad &lt;-&gt; DTO para Proyecto.
 * <p>
 * <b>Nota sobre el modelo asumido:</b> este mapper asume que
 * {@code Proyecto} sigue el mismo patrón Lombok ({@code @Builder} +
 * getters/setters) que {@code Cliente} y {@code Material} de Fase 1
 * (visible en {@code ClienteMapper}/{@code MaterialMapper} de Fase
 * 2b), con un campo {@code cliente} de tipo {@code Cliente}
 * (no un {@code clienteId} suelto) y un campo {@code estado} de tipo
 * {@code EstadoProyecto}, tal y como describe el contexto de esta
 * fase. Si los nombres de método de tu entidad real difieren, ajusta
 * solo este fichero — el resto de la capa de negocio no depende de
 * los detalles de Lombok.
 * <p>
 * <b>Por qué sigue siendo mapeo manual y no MapStruct:</b> ver
 * {@code fase2c-notas-tecnicas.md}, sección 5. En resumen: el único
 * elemento nuevo de complejidad frente a Fase 2b es (a) resolver
 * {@code clienteId} a una entidad {@code Cliente} —ya resuelta fuera
 * del mapper, en el servicio, porque requiere acceso a un
 * repositorio— y (b) mapear una lista anidada de un solo nivel,
 * delegando en {@code ProyectoMaterialMapper.aResponse}. Ninguna de
 * las dos justifica todavía introducir un procesador de anotaciones.
 */
public final class ProyectoMapper {

    private ProyectoMapper(){}


    /**
     * {@code cliente} ya viene resuelto desde el servicio (que es quien
     * tiene acceso a {@code ClienteRepository} y decide qué excepción
     * lanzar si no existe). El mapper no conoce repositorios, igual que
     * en Fase 2b.
     */
    public static Proyecto aEntidad(ProyectoRequest request, Cliente cliente){
        return Proyecto.builder()
                .nombre(request.nombre())
                .cliente(cliente)
                .fechaInicio(request.fechaInicio())
                .fechaEntregaPrevista(request.fechaEntregaPrevista())
                .presupuesto(request.presupuesto())
                .build();
        // Deliberadamente NO se fija "estado" aquí: en creación, el estado
        // inicial (EN_CURSO) lo fuerza ProyectoService.crear() después de
        // llamar a este método, para que la regla de negocio "todo proyecto
        // nace EN_CURSO" esté en una sola línea explícita en el servicio y
        // no escondida dentro del mapper.
    }

    /**
     * Copia los campos editables de {@code request} sobre una entidad ya
     * persistida, conservando su {@code id}. El cambio de estado ya se
     * valida en el servicio (RN-03) antes de llamar a este método: aquí
     * solo se aplica el valor, igual que con el resto de campos.
     */
    public static void actualizarEntidad(Proyecto proyecto, ProyectoRequest request, Cliente cliente) {
        proyecto.setNombre(request.nombre());
        proyecto.setCliente(cliente);
        proyecto.setFechaInicio(request.fechaInicio());
        proyecto.setFechaEntregaPrevista(request.fechaEntregaPrevista());
        proyecto.setEstado(request.estado());
        proyecto.setPresupuesto(request.presupuesto());
    }

    /**
     * Versión "resumen", sin materiales asignados. Se usa en el listado
     * paginado ({@code GET /api/v1/proyectos}) para no disparar una
     * consulta adicional por cada fila de la página. Ver
     * {@code fase2c-notas-tecnicas.md}, sección 5.
     */
    public static ProyectoResponse aResponse(Proyecto proyecto) {
        return construirResponse(proyecto, Collections.emptyList());
    }

    /**
     * Versión "detalle", con la lista de materiales asignados ya
     * cargada por el servicio. Se usa en
     * {@code GET /api/v1/proyectos/{id}}, donde el documento de
     * requisitos exige explícitamente incluir los materiales.
     */
    public static ProyectoResponse aResponseDetalle(Proyecto proyecto, List<ProyectoMaterial> materiales) {
        return construirResponse(proyecto, materiales.stream()
                .map(ProyectoMaterialMapper::aResponse)
                .toList());
    }

    private static ProyectoResponse construirResponse(Proyecto proyecto, List<ProyectoMaterialResponse> materiales) {
        Cliente cliente = proyecto.getCliente();
        return new ProyectoResponse(
                proyecto.getId(),
                proyecto.getNombre(),
                cliente.getId(),
                cliente.getNombre(),
                proyecto.getFechaInicio(),
                proyecto.getFechaEntregaPrevista(),
                proyecto.getEstado(),
                proyecto.getPresupuesto(),
                materiales
        );
    }
}
