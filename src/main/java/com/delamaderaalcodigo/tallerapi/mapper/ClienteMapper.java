package com.delamaderaalcodigo.tallerapi.mapper;

import com.delamaderaalcodigo.tallerapi.dto.ClienteRequest;
import com.delamaderaalcodigo.tallerapi.dto.ClienteResponse;
import com.delamaderaalcodigo.tallerapi.model.Cliente;

/**
 * Conversión manual entidad &lt;-&gt; DTO para Cliente. Ver
 * fase2b-notas-tecnicas.md, sección 2, para la justificación de
 * mapeo manual frente a una librería como MapStruct.
 */
public final class ClienteMapper {

    private ClienteMapper() {
        // Clase de utilidad: no se instancia.
    }

    public static Cliente aEntidad(ClienteRequest request) {
        return Cliente.builder()
                .nombre(request.nombre())
                .email(request.email())
                .telefono(request.telefono())
                .build();
    }

    /**
     * Copia los campos editables de {@code request} sobre una entidad
     * ya persistida, conservando su {@code id}. Se usa en
     * actualizaciones (PUT) para no perder la identidad del registro.
     */
    public static void actualizarEntidad(Cliente cliente, ClienteRequest request) {
        cliente.setNombre(request.nombre());
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
    }

    public static ClienteResponse aResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getTelefono()
        );
    }
}