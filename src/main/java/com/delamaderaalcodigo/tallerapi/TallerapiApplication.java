package com.delamaderaalcodigo.tallerapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Punto de entrada de la aplicación TallerAPI
 *
 * <p>TallerAPI es un microservicio RESTpara la gestión de inventario y
 * proyecto de un taller de carpintería. Esta clase únicamnete arranca
 * el contexto de Spring Boot; toda la configuración específica
 * (Seguridad, OpenAPI, datasource, etc.) vive en el patete
 * {@code config} y en {@code application.yml}.</p>
 */
@SpringBootApplication
public class TallerapiApplication {

	public static void main(String[] args) {

		SpringApplication.run(TallerapiApplication.class, args);
	}

}
