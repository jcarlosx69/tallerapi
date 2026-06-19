package com.delamaderaalcodigo.tallerapi.exception;

import com.delamaderaalcodigo.tallerapi.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Punto único de traducción de excepciones a respuestas HTTP, usando
 * siempre el mismo formato de error ({@link ErrorResponse}) definido
 * en Fase 2a. Ver fase2b-notas-tecnicas.md, sección 4, para el
 * recorrido completo de una petición que termina aquí.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RecursoNoEncontradoException ex,
                                                             HttpServletRequest request) {
        return construirRespuesta(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage(), request);
    }

    // --- Añadidos en Fase 2c: traducción de las reglas de negocio RN-01 a RN-06 ---

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ErrorResponse> manejarStockInsuficiente(StockInsuficienteException ex,
                                                                  HttpServletRequest request) {
        // RN-01: 409, no 400. El cuerpo de la petición es válido; el conflicto
        // es con el estado actual del inventario, no con la forma de los datos.
        return construirRespuesta(HttpStatus.CONFLICT, "Conflicto de stock", ex.getMessage(), request);
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarTransicionInvalida(TransicionEstadoInvalidaException ex,
                                                                   HttpServletRequest request) {
        // RN-03: 400. Dado el estado actual del recurso, esta petición nunca
        // sería válida (no es un conflicto temporal).
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Transición de estado inválida", ex.getMessage(), request);
    }

    @ExceptionHandler(RecursoEnUsoException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoEnUso(RecursoEnUsoException ex,
                                                             HttpServletRequest request) {
        // RN-04: 409. El recurso existe y la petición está bien formada, pero
        // tiene dependencias activas que impiden eliminarlo ahora mismo.
        return construirRespuesta(HttpStatus.CONFLICT, "Recurso en uso", ex.getMessage(), request);
    }

    @ExceptionHandler(FechaEntregaInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarFechaEntregaInvalida(FechaEntregaInvalidaException ex,
                                                                     HttpServletRequest request) {
        // RN-06: 400, mismo motivo que MethodArgumentNotValidException de abajo,
        // pero como excepción propia porque la comparación es entre dos campos
        // del propio DTO y se resuelve en el servicio, no con Bean Validation.
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Error de validación", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        // ErrorResponse solo tiene un campo "message" (no una lista de errores por
        // campo): condensamos todos los fallos de validación en una sola cadena
        // legible "campo: motivo; campo2: motivo2" en lugar de ampliar el contrato
        // de ErrorResponse ya fijado en Fase 2a. Trade-off explicado en las notas.
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatearError)
                .collect(Collectors.joining("; "));

        return construirRespuesta(HttpStatus.BAD_REQUEST, "Error de validación", detalle, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGenerico(Exception ex, HttpServletRequest request) {
        // Se loguea la excepción completa (con stack trace) en el servidor para
        // poder diagnosticarla, aunque NUNCA se exponga ex.getMessage() al
        // cliente: el mensaje genérico evita filtrar detalles internos (rutas
        // de fichero, SQL, stack traces) a un cliente externo.
        log.error("Error no controlado en al procesar {} {}", request.getMethod(), request.getRequestURI(), ex);

        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ha ocurrido un error inesperado. Contacte con el administrador.", request);
    }

    private ResponseEntity<ErrorResponse> construirRespuesta(HttpStatus status, String error,
                                                             String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    private String formatearError(FieldError fieldError) {
        return "%s: %s".formatted(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
