package com.delamaderaalcodigo.tallerapi.controller;

import com.delamaderaalcodigo.tallerapi.dto.AsignarMaterialRequest;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoMaterialResponse;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoRequest;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoResponse;
import com.delamaderaalcodigo.tallerapi.model.EstadoProyecto;
import com.delamaderaalcodigo.tallerapi.service.ProyectoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/proyectos")
@Tag(name = "Proyectos", description = "Gestión de proyectos del taller y asignación de materiales")
public class ProyectoController {

    private final ProyectoService proyectoService;

    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @GetMapping
    @Operation(summary = "Listado paginado de proyectos, con filtros opcionales por estado y rango de fechas de inicio")
    public ResponseEntity<Page<ProyectoResponse>> listar(
            @Parameter(description = "Filtra por estado: EN_CURSO, TERMINADO, ENTREGADO o CANCELADO")
            @RequestParam(required = false) EstadoProyecto estado,
            @Parameter(description = "Filtra proyectos con fechaInicio >= fechaDesde. " +
                    "Debe enviarse junto con fechaHasta para que el filtro tenga efecto")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @Parameter(description = "Filtra proyectos con fechaInicio <= fechaHasta. " +
                    "Debe enviarse junto con fechaDesde para que el filtro tenga efecto")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable pageable) {
        return ResponseEntity.ok(proyectoService.listar(estado, fechaDesde, fechaHasta, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener el detalle de un proyecto, incluyendo sus materiales asignados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un proyecto con ese id")
    })
    public ResponseEntity<ProyectoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(proyectoService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo proyecto, siempre en estado EN_CURSO (requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proyecto creado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos, o fechaEntregaPrevista anterior a fechaInicio"),
            @ApiResponse(responseCode = "404", description = "No existe el cliente indicado")
    })
    public ResponseEntity<ProyectoResponse> crear(@Valid @RequestBody ProyectoRequest request) {
        ProyectoResponse creado = proyectoService.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/proyectos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un proyecto existente, incluyendo cambio de estado (requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, transición de estado no permitida (RN-03), o fecha de entrega inválida (RN-06)"),
            @ApiResponse(responseCode = "404", description = "No existe el proyecto o el cliente indicado")
    })
    public ResponseEntity<ProyectoResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody ProyectoRequest request) {
        return ResponseEntity.ok(proyectoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un proyecto, reponiendo el stock de sus materiales asignados (requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proyecto eliminado"),
            @ApiResponse(responseCode = "404", description = "No existe un proyecto con ese id")
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        proyectoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/materiales")
    @Operation(summary = "Asignar un material al proyecto, validando stock disponible (requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Material asignado, stock descontado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "No existe el proyecto o el material indicado"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente (RN-01)")
    })
    public ResponseEntity<ProyectoMaterialResponse> asignarMaterial(@PathVariable Long id,
                                                                    @Valid @RequestBody AsignarMaterialRequest request) {
        ProyectoMaterialResponse asignado = proyectoService.asignarMaterial(id, request);
        return ResponseEntity.created(URI.create("/api/v1/proyectos/" + id + "/materiales")).body(asignado);
    }

    @GetMapping("/{id}/materiales")
    @Operation(summary = "Listar los materiales asignados a un proyecto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de materiales asignados"),
            @ApiResponse(responseCode = "404", description = "No existe un proyecto con ese id")
    })
    public ResponseEntity<List<ProyectoMaterialResponse>> listarMaterialesAsignados(@PathVariable Long id) {
        return ResponseEntity.ok(proyectoService.listarMaterialesAsignados(id));
    }

    @DeleteMapping("/{id}/materiales/{materialId}")
    @Operation(summary = "Revertir la asignación de un material, reponiendo stock (requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Asignación revertida, stock repuesto"),
            @ApiResponse(responseCode = "404", description = "No existe el proyecto, o no tiene asignado ese material")
    })
    public ResponseEntity<Void> desasignarMaterial(@PathVariable Long id, @PathVariable Long materialId) {
        proyectoService.desasignarMaterial(id, materialId);
        return ResponseEntity.noContent().build();
    }
}
