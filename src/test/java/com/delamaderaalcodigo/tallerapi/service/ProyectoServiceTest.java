package com.delamaderaalcodigo.tallerapi.service;

import com.delamaderaalcodigo.tallerapi.dto.AsignarMaterialRequest;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoMaterialResponse;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoRequest;
import com.delamaderaalcodigo.tallerapi.dto.ProyectoResponse;
import com.delamaderaalcodigo.tallerapi.exception.FechaEntregaInvalidaException;
import com.delamaderaalcodigo.tallerapi.exception.RecursoNoEncontradoException;
import com.delamaderaalcodigo.tallerapi.exception.StockInsuficienteException;
import com.delamaderaalcodigo.tallerapi.exception.TransicionEstadoInvalidaException;
import com.delamaderaalcodigo.tallerapi.model.Cliente;
import com.delamaderaalcodigo.tallerapi.model.EstadoProyecto;
import com.delamaderaalcodigo.tallerapi.model.Material;
import com.delamaderaalcodigo.tallerapi.model.Proyecto;
import com.delamaderaalcodigo.tallerapi.model.ProyectoMaterial;
import com.delamaderaalcodigo.tallerapi.model.TipoMaterial;
import com.delamaderaalcodigo.tallerapi.model.UnidadMedida;
import com.delamaderaalcodigo.tallerapi.repository.ClienteRepository;
import com.delamaderaalcodigo.tallerapi.repository.MaterialRepository;
import com.delamaderaalcodigo.tallerapi.repository.ProyectoMaterialRepository;
import com.delamaderaalcodigo.tallerapi.repository.ProyectoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link ProyectoService} con Mockito.
 *
 * <p>Convención de nombres de método de test usada en todo el proyecto a partir de esta
 * fase: {@code deberia_<resultado>_cuando_<condicion>}. Se agrupan los tests con
 * {@code @Nested} por método de servicio o por regla de negocio (RN-XX) cuando un método
 * cubre varias reglas distintas, para que el árbol de tests de un IDE se lea como un índice
 * de comportamientos en lugar de una lista plana de nombres largos.</p>
 *
 * <p>Ver {@code fase2d1-notas-tecnicas.md} para la justificación de por qué se testea con
 * {@code @ExtendWith(MockitoExtension.class)} en lugar de {@code @SpringBootTest}.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProyectoService")
class ProyectoServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private ProyectoMaterialRepository proyectoMaterialRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private ProyectoService proyectoService;

    // ---------------------------------------------------------------
    // Helpers de construcción de entidades y DTOs de prueba.
    // Centralizarlos aquí evita repetir "builders" largos en cada test
    // y deja claro, de un vistazo, qué valores son realmente relevantes
    // para cada caso (los que se pasan como parámetro) frente a los que
    // son solo "relleno" necesario para que la entidad sea válida.
    // ---------------------------------------------------------------

    private static Cliente clienteDePrueba(Long id) {
        return Cliente.builder()
                .id(id)
                .nombre("Cliente de prueba " + id)
                .email("cliente" + id + "@correo.com")
                .telefono("600000000")
                .build();
    }

    private static Material materialDePrueba(Long id, BigDecimal stockDisponible) {
        return Material.builder()
                .id(id)
                .nombre("Tablero de pino")
                .tipo(TipoMaterial.MADERA)
                .unidad(UnidadMedida.METRO)
                .stockDisponible(stockDisponible)
                .costeUnitario(new BigDecimal("12.50"))
                .build();
    }

    private static Proyecto proyectoDePrueba(Long id, EstadoProyecto estado, Cliente cliente) {
        return Proyecto.builder()
                .id(id)
                .nombre("Mesa de comedor a medida")
                .cliente(cliente)
                .fechaInicio(LocalDate.of(2026, 1, 10))
                .fechaEntregaPrevista(LocalDate.of(2026, 2, 10))
                .estado(estado)
                .presupuesto(new BigDecimal("850.00"))
                .build();
    }

    private static ProyectoMaterial asignacionDePrueba(Long id, Proyecto proyecto, Material material,
                                                       BigDecimal cantidad) {
        return ProyectoMaterial.builder()
                .id(id)
                .proyecto(proyecto)
                .material(material)
                .cantidadAsignada(cantidad)
                .fechaAsignacion(LocalDateTime.of(2026, 1, 15, 10, 30))
                .build();
    }

    private static ProyectoRequest requestDePrueba(Long clienteId, EstadoProyecto estado,
                                                   LocalDate fechaInicio, LocalDate fechaEntregaPrevista) {
        return new ProyectoRequest("Mesa de comedor a medida", clienteId, fechaInicio, fechaEntregaPrevista,
                estado, new BigDecimal("850.00"));
    }

    @Nested
    @DisplayName("crear()")
    class Crear {

        @Test
        @DisplayName("deberia_forzarEstadoEnCurso_cuandoSeCreaUnProyecto_independientementeDelEstadoEnElRequest")
        void deberia_forzarEstadoEnCurso_cuandoSeCreaUnProyecto_independientementeDelEstadoEnElRequest() {
            // Arrange: el request pide explícitamente un estado distinto de EN_CURSO, para
            // comprobar que el servicio lo ignora (regla "todo proyecto nace EN_CURSO").
            Cliente cliente = clienteDePrueba(1L);
            ProyectoRequest request = requestDePrueba(1L, EstadoProyecto.ENTREGADO,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(proyectoRepository.save(any(Proyecto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

            // Act
            ProyectoResponse response = proyectoService.crear(request);

            // Assert: comprobamos tanto la entidad que llegó a save() como la respuesta
            // devuelta al llamante, para cubrir ambos extremos de la transformación.
            ArgumentCaptor<Proyecto> captor = ArgumentCaptor.forClass(Proyecto.class);
            verify(proyectoRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoProyecto.EN_CURSO);
            assertThat(response.estado()).isEqualTo(EstadoProyecto.EN_CURSO);
        }
    }

    @Nested
    @DisplayName("RN-06: validación de fecha de entrega prevista (vía crear())")
    class Rn06ValidacionFechaEntrega {

        @Test
        @DisplayName("deberia_lanzarFechaEntregaInvalida_cuandoLaFechaDeEntregaEsAnteriorALaFechaDeInicio")
        void deberia_lanzarFechaEntregaInvalida_cuandoLaFechaDeEntregaEsAnteriorALaFechaDeInicio() {
            // Arrange
            Cliente cliente = clienteDePrueba(1L);
            LocalDate fechaInicio = LocalDate.of(2026, 6, 10);
            LocalDate fechaEntregaAnterior = LocalDate.of(2026, 6, 1);
            ProyectoRequest request = requestDePrueba(1L, EstadoProyecto.EN_CURSO, fechaInicio, fechaEntregaAnterior);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

            // Act & Assert
            assertThatThrownBy(() -> proyectoService.crear(request))
                    .isInstanceOf(FechaEntregaInvalidaException.class)
                    .hasMessageContaining("no puede ser anterior");

            // El proyecto inválido no debe haber llegado a persistirse.
            verify(proyectoRepository, never()).save(any());
        }

        @ParameterizedTest(name = "fechaEntregaPrevista = {0} (igual o posterior a fechaInicio) no lanza excepción")
        @DisplayName("deberia_noLanzarExcepcion_cuandoLaFechaDeEntregaEsIgualOPosteriorALaFechaDeInicio")
        @ValueSource(strings = {"2026-06-10", "2026-06-15"}) // igual a fechaInicio / posterior
        void deberia_noLanzarExcepcion_cuandoLaFechaDeEntregaEsIgualOPosteriorALaFechaDeInicio(
                String fechaEntregaTexto) {
            // Arrange
            Cliente cliente = clienteDePrueba(1L);
            LocalDate fechaInicio = LocalDate.of(2026, 6, 10);
            LocalDate fechaEntrega = LocalDate.parse(fechaEntregaTexto);
            ProyectoRequest request = requestDePrueba(1L, EstadoProyecto.EN_CURSO, fechaInicio, fechaEntrega);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(proyectoRepository.save(any(Proyecto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

            // Act & Assert
            assertThatCode(() -> proyectoService.crear(request)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deberia_noLanzarExcepcion_cuandoLaFechaDeEntregaPrevistaEsNula")
        void deberia_noLanzarExcepcion_cuandoLaFechaDeEntregaPrevistaEsNula() {
            // Arrange: fechaEntregaPrevista es opcional según el modelo de datos; RN-06
            // solo se aplica "si se especifica".
            Cliente cliente = clienteDePrueba(1L);
            ProyectoRequest request = requestDePrueba(1L, EstadoProyecto.EN_CURSO, LocalDate.of(2026, 6, 10), null);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(proyectoRepository.save(any(Proyecto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

            // Act & Assert
            assertThatCode(() -> proyectoService.crear(request)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("obtenerPorId()")
    class ObtenerPorId {

        @Test
        @DisplayName("deberia_devolverElDetalleConLosMaterialesAsignados_cuandoElProyectoExiste")
        void deberia_devolverElDetalleConLosMaterialesAsignados_cuandoElProyectoExiste() {
            // Arrange
            Cliente cliente = clienteDePrueba(1L);
            Proyecto proyecto = proyectoDePrueba(10L, EstadoProyecto.EN_CURSO, cliente);
            Material material = materialDePrueba(5L, new BigDecimal("100"));
            ProyectoMaterial asignacion = asignacionDePrueba(1L, proyecto, material, new BigDecimal("3"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(proyectoMaterialRepository.findByProyectoId(10L)).thenReturn(List.of(asignacion));

            // Act
            ProyectoResponse response = proyectoService.obtenerPorId(10L);

            // Assert
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.materiales()).hasSize(1);
            assertThat(response.materiales().get(0).materialId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("deberia_lanzarRecursoNoEncontrado_cuandoElProyectoNoExiste")
        void deberia_lanzarRecursoNoEncontrado_cuandoElProyectoNoExiste() {
            // Arrange
            when(proyectoRepository.findById(404L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> proyectoService.obtenerPorId(404L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("404");

            verify(proyectoMaterialRepository, never()).findByProyectoId(any());
        }
    }

    @Nested
    @DisplayName("RN-03: transición de estados de Proyecto (vía actualizar())")
    class Rn03TransicionDeEstados {

        @ParameterizedTest(name = "{0} -> {1} : ¿transición permitida? {2}")
        @DisplayName("deberia_validarLaTransicionDeEstadoSegunLaMatrizDeRn03")
        @CsvSource({
                // Flujo normal.
                "EN_CURSO,   TERMINADO, true",
                "TERMINADO,  ENTREGADO, true",
                // CANCELADO es alcanzable desde EN_CURSO y TERMINADO...
                "EN_CURSO,   CANCELADO, true",
                "TERMINADO,  CANCELADO, true",
                // ...pero NO desde ENTREGADO (criterio documentado en ProyectoService:
                // una vez entregado el encargo, no tiene sentido cancelarlo).
                "ENTREGADO,  CANCELADO, false",
                // "Mismo estado" es un caso especial: no es una transición sino un no-op,
                // y el servicio lo permite explícitamente sin ni siquiera consultar la
                // matriz de transiciones válidas.
                "EN_CURSO,   EN_CURSO,  true",
                "TERMINADO,  TERMINADO, true",
                "ENTREGADO,  ENTREGADO, true",
                "CANCELADO,  CANCELADO, true",
                // Transiciones inválidas: ENTREGADO y CANCELADO son estados terminales, y
                // no se puede retroceder en el flujo normal.
                "TERMINADO,  EN_CURSO,  false",
                "ENTREGADO,  EN_CURSO,  false",
                "ENTREGADO,  TERMINADO, false",
                "CANCELADO,  EN_CURSO,  false",
                "CANCELADO,  TERMINADO, false",
                "CANCELADO,  ENTREGADO, false",
        })
        void deberia_validarLaTransicionDeEstadoSegunLaMatrizDeRn03(
                EstadoProyecto estadoActual, EstadoProyecto estadoDestino, boolean esPermitida) {
            // Arrange
            Cliente cliente = clienteDePrueba(1L);
            Proyecto proyectoExistente = proyectoDePrueba(10L, estadoActual, cliente);
            ProyectoRequest request = requestDePrueba(1L, estadoDestino,
                    proyectoExistente.getFechaInicio(), proyectoExistente.getFechaEntregaPrevista());

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyectoExistente));
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

            // Act & Assert
            if (esPermitida) {
                assertThatCode(() -> proyectoService.actualizar(10L, request)).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> proyectoService.actualizar(10L, request))
                        .isInstanceOf(TransicionEstadoInvalidaException.class);
            }
        }
    }

    @Nested
    @DisplayName("eliminar()")
    class Eliminar {

        @Test
        @DisplayName("deberia_reponerStockDeTodasLasAsignaciones_yEliminarlas_antesDeEliminarElProyecto")
        void deberia_reponerStockDeTodasLasAsignaciones_yEliminarlas_antesDeEliminarElProyecto() {
            // Arrange
            Cliente cliente = clienteDePrueba(1L);
            Proyecto proyecto = proyectoDePrueba(10L, EstadoProyecto.EN_CURSO, cliente);
            Material materialA = materialDePrueba(1L, new BigDecimal("10"));
            Material materialB = materialDePrueba(2L, new BigDecimal("20"));
            ProyectoMaterial asignacionA = asignacionDePrueba(100L, proyecto, materialA, new BigDecimal("4"));
            ProyectoMaterial asignacionB = asignacionDePrueba(101L, proyecto, materialB, new BigDecimal("7"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(proyectoMaterialRepository.findByProyectoId(10L)).thenReturn(List.of(asignacionA, asignacionB));

            // Act
            proyectoService.eliminar(10L);

            // Assert: el stock de cada material se repone con la cantidad de su propia
            // asignación, no con un valor compartido.
            assertThat(materialA.getStockDisponible()).isEqualByComparingTo("14"); // 10 + 4
            assertThat(materialB.getStockDisponible()).isEqualByComparingTo("27"); // 20 + 7

            verify(proyectoMaterialRepository).delete(asignacionA);
            verify(proyectoMaterialRepository).delete(asignacionB);
            verify(proyectoRepository).delete(proyecto);
        }

        @Test
        @DisplayName("deberia_lanzarRecursoNoEncontrado_cuandoElProyectoAEliminarNoExiste")
        void deberia_lanzarRecursoNoEncontrado_cuandoElProyectoAEliminarNoExiste() {
            // Arrange
            when(proyectoRepository.findById(404L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> proyectoService.eliminar(404L))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(proyectoMaterialRepository, never()).findByProyectoId(any());
            verify(proyectoRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("asignarMaterial() — RN-01: validación de stock")
    class Rn01AsignacionDeMaterial {

        @Test
        @DisplayName("deberia_descontarStockYGuardarLaAsignacion_cuandoHayStockSuficiente")
        void deberia_descontarStockYGuardarLaAsignacion_cuandoHayStockSuficiente() {
            // --- Arrange ---
            // Un proyecto existente y un material con stock de sobra para la cantidad
            // que se va a solicitar.
            Cliente cliente = clienteDePrueba(1L);
            Proyecto proyecto = proyectoDePrueba(10L, EstadoProyecto.EN_CURSO, cliente);
            Material material = materialDePrueba(5L, new BigDecimal("10"));
            AsignarMaterialRequest request = new AsignarMaterialRequest(5L, new BigDecimal("4"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(materialRepository.findById(5L)).thenReturn(Optional.of(material));
            when(proyectoMaterialRepository.save(any(ProyectoMaterial.class)))
                    .thenAnswer(invocacion -> invocacion.getArgument(0));

            // --- Act ---
            ProyectoMaterialResponse response = proyectoService.asignarMaterial(10L, request);

            // --- Assert ---
            // 1. El stock del material se ha descontado en la cantidad solicitada.
            assertThat(material.getStockDisponible()).isEqualByComparingTo("6"); // 10 - 4
            // 2. La respuesta devuelta refleja la cantidad asignada.
            assertThat(response.cantidadAsignada()).isEqualByComparingTo("4");
            // 3. La asignación que llega a save() trae fechaAsignacion ya rellena (no
            //    null): se fija en el servicio, no se delega en @PrePersist ni en la BD,
            //    así que un test que comparase contra un objeto con fechaAsignacion=null
            //    fallaría de forma engañosa por un detalle ajeno a lo que se quiere probar.
            ArgumentCaptor<ProyectoMaterial> captor = ArgumentCaptor.forClass(ProyectoMaterial.class);
            verify(proyectoMaterialRepository).save(captor.capture());
            assertThat(captor.getValue().getFechaAsignacion()).isNotNull();
        }

        @Test
        @DisplayName("deberia_permitirLaAsignacion_cuandoLaCantidadSolicitadaEsExactamenteIgualAlStockDisponible")
        void deberia_permitirLaAsignacion_cuandoLaCantidadSolicitadaEsExactamenteIgualAlStockDisponible() {
            // Caso límite: el servicio solo lanza StockInsuficienteException cuando
            // cantidadAsignada > stockDisponible (comparación estricta), así que la
            // igualdad exacta debe ser un caso válido, no el límite del error.
            Cliente cliente = clienteDePrueba(1L);
            Proyecto proyecto = proyectoDePrueba(10L, EstadoProyecto.EN_CURSO, cliente);
            Material material = materialDePrueba(5L, new BigDecimal("4"));
            AsignarMaterialRequest request = new AsignarMaterialRequest(5L, new BigDecimal("4"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(materialRepository.findById(5L)).thenReturn(Optional.of(material));
            when(proyectoMaterialRepository.save(any(ProyectoMaterial.class)))
                    .thenAnswer(invocacion -> invocacion.getArgument(0));

            assertThatCode(() -> proyectoService.asignarMaterial(10L, request)).doesNotThrowAnyException();
            assertThat(material.getStockDisponible()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("deberia_lanzarStockInsuficiente_yNoModificarElStockNiGuardarNada_cuandoLaCantidadExcedeElStock")
        void deberia_lanzarStockInsuficiente_yNoModificarElStockNiGuardarNada_cuandoLaCantidadExcedeElStock() {
            // Arrange
            Cliente cliente = clienteDePrueba(1L);
            Proyecto proyecto = proyectoDePrueba(10L, EstadoProyecto.EN_CURSO, cliente);
            Material material = materialDePrueba(5L, new BigDecimal("3"));
            AsignarMaterialRequest request = new AsignarMaterialRequest(5L, new BigDecimal("999"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(materialRepository.findById(5L)).thenReturn(Optional.of(material));

            // Act & Assert
            assertThatThrownBy(() -> proyectoService.asignarMaterial(10L, request))
                    .isInstanceOf(StockInsuficienteException.class)
                    .hasMessageContaining("Stock insuficiente");

            // El stock no debe haberse tocado, y no debe haberse guardado ninguna asignación:
            // si la validación falla, la operación no debe tener ningún efecto observable.
            assertThat(material.getStockDisponible()).isEqualByComparingTo("3");
            verify(proyectoMaterialRepository, never()).save(any());
        }

        @Test
        @DisplayName("deberia_lanzarRecursoNoEncontrado_cuandoElMaterialAAsignarNoExiste")
        void deberia_lanzarRecursoNoEncontrado_cuandoElMaterialAAsignarNoExiste() {
            // Arrange
            Cliente cliente = clienteDePrueba(1L);
            Proyecto proyecto = proyectoDePrueba(10L, EstadoProyecto.EN_CURSO, cliente);
            AsignarMaterialRequest request = new AsignarMaterialRequest(999L, new BigDecimal("1"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(materialRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> proyectoService.asignarMaterial(10L, request))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(proyectoMaterialRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("desasignarMaterial() — RN-02: reposición de stock")
    class Rn02DesasignacionDeMaterial {

        @Test
        @DisplayName("deberia_reponerStockYEliminarLaAsignacion_cuandoElProyectoTieneEseMaterialAsignado")
        void deberia_reponerStockYEliminarLaAsignacion_cuandoElProyectoTieneEseMaterialAsignado() {
            // Arrange
            Cliente cliente = clienteDePrueba(1L);
            Proyecto proyecto = proyectoDePrueba(10L, EstadoProyecto.EN_CURSO, cliente);
            Material material = materialDePrueba(5L, new BigDecimal("6"));
            ProyectoMaterial asignacion = asignacionDePrueba(100L, proyecto, material, new BigDecimal("4"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(proyectoMaterialRepository.findByProyectoId(10L)).thenReturn(List.of(asignacion));

            // Act
            proyectoService.desasignarMaterial(10L, 5L);

            // Assert
            assertThat(material.getStockDisponible()).isEqualByComparingTo("10"); // 6 + 4
            verify(proyectoMaterialRepository).delete(asignacion);
        }

        @Test
        @DisplayName("deberia_localizarLaAsignacionCorrecta_cuandoElProyectoTieneVariasAsignaciones")
        void deberia_localizarLaAsignacionCorrecta_cuandoElProyectoTieneVariasAsignaciones() {
            // El repositorio no tiene un findByProyectoIdAndMaterialId: el servicio filtra
            // en memoria sobre la lista completa de asignaciones del proyecto (ver el
            // comentario en ProyectoService.desasignarMaterial). Este test comprueba que
            // selecciona la asignación correcta y deja las demás completamente intactas.
            Cliente cliente = clienteDePrueba(1L);
            Proyecto proyecto = proyectoDePrueba(10L, EstadoProyecto.EN_CURSO, cliente);
            Material materialBuscado = materialDePrueba(5L, new BigDecimal("6"));
            Material otroMaterial = materialDePrueba(6L, new BigDecimal("20"));
            ProyectoMaterial asignacionBuscada = asignacionDePrueba(100L, proyecto, materialBuscado,
                    new BigDecimal("4"));
            ProyectoMaterial otraAsignacion = asignacionDePrueba(101L, proyecto, otroMaterial,
                    new BigDecimal("9"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(proyectoMaterialRepository.findByProyectoId(10L))
                    .thenReturn(List.of(otraAsignacion, asignacionBuscada));

            // Act
            proyectoService.desasignarMaterial(10L, 5L);

            // Assert
            assertThat(materialBuscado.getStockDisponible()).isEqualByComparingTo("10");
            assertThat(otroMaterial.getStockDisponible()).isEqualByComparingTo("20"); // sin cambios
            verify(proyectoMaterialRepository).delete(asignacionBuscada);
            verify(proyectoMaterialRepository, never()).delete(otraAsignacion);
        }

        @Test
        @DisplayName("deberia_lanzarRecursoNoEncontrado_cuandoElProyectoNoTieneAsignadoEseMaterial")
        void deberia_lanzarRecursoNoEncontrado_cuandoElProyectoNoTieneAsignadoEseMaterial() {
            // Arrange: el proyecto existe y tiene asignaciones, pero ninguna del material
            // que se intenta desasignar.
            Cliente cliente = clienteDePrueba(1L);
            Proyecto proyecto = proyectoDePrueba(10L, EstadoProyecto.EN_CURSO, cliente);
            Material otroMaterial = materialDePrueba(6L, new BigDecimal("20"));
            ProyectoMaterial otraAsignacion = asignacionDePrueba(101L, proyecto, otroMaterial,
                    new BigDecimal("9"));

            when(proyectoRepository.findById(10L)).thenReturn(Optional.of(proyecto));
            when(proyectoMaterialRepository.findByProyectoId(10L)).thenReturn(List.of(otraAsignacion));

            // Act & Assert
            assertThatThrownBy(() -> proyectoService.desasignarMaterial(10L, 999L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("no tiene asignado");

            verify(proyectoMaterialRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deberia_lanzarRecursoNoEncontrado_cuandoElProyectoNoExiste")
        void deberia_lanzarRecursoNoEncontrado_cuandoElProyectoNoExiste() {
            // Arrange
            when(proyectoRepository.findById(404L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> proyectoService.desasignarMaterial(404L, 1L))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(proyectoMaterialRepository, never()).findByProyectoId(any());
        }
    }
}
