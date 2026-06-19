package com.delamaderaalcodigo.tallerapi.mapper;

import com.delamaderaalcodigo.tallerapi.dto.MaterialRequest;
import com.delamaderaalcodigo.tallerapi.dto.MaterialResponse;
import com.delamaderaalcodigo.tallerapi.model.Material;

public final class MaterialMapper {

    private MaterialMapper() {
        // Clase de utilidad: no se instancia.
    }

    public static Material aEntidad(MaterialRequest request) {
        return Material.builder()
                .nombre(request.nombre())
                .tipo(request.tipo())
                .unidad(request.unidad())
                .stockDisponible(request.stockDisponible())
                .costeUnitario(request.costeUnitario())
                .build();
    }

    public static void actualizarEntidad(Material material, MaterialRequest request) {
        material.setNombre(request.nombre());
        material.setTipo(request.tipo());
        material.setUnidad(request.unidad());
        material.setStockDisponible(request.stockDisponible());
        material.setCosteUnitario(request.costeUnitario());
    }

    public static MaterialResponse aResponse(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getNombre(),
                material.getTipo(),
                material.getUnidad(),
                material.getStockDisponible(),
                material.getCosteUnitario()
        );
    }
}