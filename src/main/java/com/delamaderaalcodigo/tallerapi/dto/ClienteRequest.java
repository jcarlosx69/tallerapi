package com.delamaderaalcodigo.tallerapi.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear/actualizar un Cliente. Sin {@code id}:
 * el id lo asigna la base de datos (creación) o viene el la URL
 * (actualización), nunca en el cuerpo de la petición.
 */
public record ClienteRequest (
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Email(message = "El email debe tener un formato válido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefono
){
}
