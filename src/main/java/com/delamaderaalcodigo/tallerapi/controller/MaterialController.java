package com.delamaderaalcodigo.tallerapi.controller;

import com.delamaderaalcodigo.tallerapi.dto.MaterialRequest;
import com.delamaderaalcodigo.tallerapi.dto.MaterialResponse;
import com.delamaderaalcodigo.tallerapi.model.TipoMaterial;
import com.delamaderaalcodigo.tallerapi.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/materiales")
@Tag(name = "Materiales", description = "CRUD de materiales de inventario del taller")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    @Operation(summary = "Listado paginado de materiales, con filtro opcional por tipo")
    public ResponseEntity<Page<MaterialResponse>> listar(
            @Parameter(description = "Filtra por tipo de material: MADERA, HERRAJE, BARNIZ u OTRO")
            @RequestParam(required = false) TipoMaterial tipo,
            Pageable pageable) {
        return ResponseEntity.ok(materialService.listar(tipo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un material por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Material encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un material con ese id")
    })
    public ResponseEntity<MaterialResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo material (requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Material creado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<MaterialResponse> crear(@Valid @RequestBody MaterialRequest request) {
        MaterialResponse creado = materialService.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/materiales/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un material existente, incluyendo stock (requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Material actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "No existe un material con ese id")
    })
    public ResponseEntity<MaterialResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.ok(materialService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un material (requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Material eliminado"),
            @ApiResponse(responseCode = "404", description = "No existe un material con ese id")
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        materialService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}