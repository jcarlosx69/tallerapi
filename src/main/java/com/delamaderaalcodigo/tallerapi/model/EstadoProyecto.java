package com.delamaderaalcodigo.tallerapi.model;

/**
 * Estados posibles del ciclo de vida de un {@link Proyecto}.
 *
 * <p>El fluja normal es EN_CURSO -&gt; TERMINADO -&gt; ENTREGADO. la
 * transición de ENTREGADO a EN_CURSO está prohibida (RN-03 del documento
 * de requisitos); esa validación se implementará  en la etapa de
 * servicio en la fase 2, pero el enum ya recoge todos los estados
 * válidos para que la base de datos los pueda restringir mediante
 * {@code CHECK}.</p>
 */
public enum EstadoProyecto {
    EN_CURSO,
    TERMINADO,
    ENTREGADO,
    CANCELADO
}
