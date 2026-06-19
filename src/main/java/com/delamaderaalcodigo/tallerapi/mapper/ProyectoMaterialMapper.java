package com.delamaderaalcodigo.tallerapi.mapper;

import com.delamaderaalcodigo.tallerapi.dto.ProyectoMaterialResponse;
import com.delamaderaalcodigo.tallerapi.model.ProyectoMaterial;

/**
 * Conversión manual entidad -&gt; DTO para ProyectoMaterial. No tiene
 * {@code aEntidad}/{@code actualizarEntidad} como los demás mappers de
 * este proyecto porque una asignación de material no se crea ni se
 * actualiza por separado: se crea como efecto de
 * {@code ProyectoService.asignarMaterial} (junto con el descuento de
 * stock, RN-01) y se elimina como efecto de
 * {@code ProyectoService.desasignarMaterial} (junto con la reposición
 * de stock, RN-02). No existe un "PUT de asignación" en la
 * especificación, así que no hay nada que mapear en esa dirección.
 */
public final class ProyectoMaterialMapper {

    private ProyectoMaterialMapper(){
        //Clase de utilidad: no se instancia.
    }

    public static ProyectoMaterialResponse aResponse(ProyectoMaterial proyectoMaterial){
        return new ProyectoMaterialResponse(
                proyectoMaterial.getId(),
                proyectoMaterial.getMaterial().getId(),
                proyectoMaterial.getMaterial().getNombre(),
                proyectoMaterial.getMaterial().getUnidad(),
                proyectoMaterial.getCantidadAsignada(),
                proyectoMaterial.getFechaAsignacion()
        );
    }
}
