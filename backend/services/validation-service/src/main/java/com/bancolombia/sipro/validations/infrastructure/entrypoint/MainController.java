package com.bancolombia.sipro.validations.infrastructure.entrypoint;

import com.bancolombia.sipro.validations.application.dto.ConsolidacionManualStatusResponse;
import com.bancolombia.sipro.validations.application.dto.ConsolidacionResumenResponse;
import com.bancolombia.sipro.validations.domain.model.Producto;
import com.bancolombia.sipro.validations.domain.model.Segmento;
import com.bancolombia.sipro.validations.domain.service.ConsolidacionConciliacionReportService;
import com.bancolombia.sipro.validations.domain.service.ConsolidacionManualAsyncService;
import com.bancolombia.sipro.validations.domain.service.ConsolidacionResumenExcelReportService;
import com.bancolombia.sipro.validations.domain.service.ConsolidacionResumenService;
import com.bancolombia.sipro.validations.infrastructure.repository.ProductoRepository;
import com.bancolombia.sipro.validations.infrastructure.repository.SegmentoRepository;
import com.bancolombia.sipro.validations.infrastructure.security.SiproAuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Expone catálogos principales y operaciones operativas de consolidación para la pantalla inicial.
 */
@RestController
@RequestMapping("/api/main")
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private SegmentoRepository segmentoRepository;

    @Autowired
    private ConsolidacionManualAsyncService consolidacionManualAsyncService;

    @Autowired
    private ConsolidacionResumenService consolidacionResumenService;

    @Autowired
    private ConsolidacionConciliacionReportService consolidacionConciliacionReportService;

    @Autowired
    private ConsolidacionResumenExcelReportService consolidacionResumenExcelReportService;

    /**
     * Lista los productos activos ordenados para poblar el catálogo del frontend.
     */
    @GetMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<List<Producto>> listarProductos(
            @AuthenticationPrincipal SiproAuthenticatedUser user,
            @RequestParam(value = "segmentoId", required = false) Long segmentoId) {
        Long idUsuario = requireAuthenticatedUserId(user);
        List<Producto> productos = productoRepository.findProductosPermitidosParaCarga(idUsuario, segmentoId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(productos);
    }

    /**
     * Lista los segmentos disponibles ordenados por nombre.
     */
    @GetMapping(value = "/segmentos", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<List<Segmento>> listarSegmentos(@AuthenticationPrincipal SiproAuthenticatedUser user) {
        Long idUsuario = requireAuthenticatedUserId(user);
        List<Segmento> segmentos = segmentoRepository.findSegmentosPermitidosParaCarga(idUsuario);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(segmentos);
    }

    /**
     * Devuelve el resumen consolidado del periodo pedido o del más reciente disponible.
     */
    @GetMapping(value = "/consolidacion/resumen", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<ConsolidacionResumenResponse> obtenerResumenConsolidacion(
            @RequestParam(value = "anio", required = false) Integer anio,
            @RequestParam(value = "mes", required = false) Integer mes) {
        ConsolidacionResumenResponse resumen = consolidacionResumenService.obtenerResumen(anio, mes);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resumen);
    }

    /**
     * Devuelve todos los registros consolidados del periodo especificado para generar
     * reportes detallados y exportaciones Excel del resumen.
     */
    @GetMapping(value = "/consolidacion/detalle-diferencia", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<?> obtenerDetalleConsolidado(
            @RequestParam(value = "anio", required = false) Integer anio,
            @RequestParam(value = "mes", required = false) Integer mes) {
        try {
            var registros = consolidacionResumenService.obtenerRegistrosConsolidadosPorPeriodo(anio, mes);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(registros);
        } catch (Exception e) {
            logger.error("Error al obtener detalle consolidado para anio={}, mes={}: {}", anio, mes, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener detalle consolidado: " + e.getMessage()));
        }
    }

        /**
         * Descarga el archivo Excel del resumen consolidado del periodo seleccionado
         * con formato tabular equivalente a la pantalla Resumen.
         */
        @GetMapping(value = "/consolidacion/resumen/reporte",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        public ResponseEntity<byte[]> descargarReporteResumenConsolidado(
            @RequestParam(value = "anio", required = false) Integer anio,
            @RequestParam(value = "mes", required = false) Integer mes) {
        try {
            ConsolidacionResumenExcelReportService.GeneratedResumenReport report =
                consolidacionResumenExcelReportService.generar(anio, mes);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + report.fileName() + "\"")
                .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(report.content());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(NOT_FOUND, ex.getMessage());
        }
        }

        /**
         * Descarga el archivo Excel de conciliación del periodo seleccionado o del último disponible.
         */
        @GetMapping(value = "/consolidacion/conciliacion/reporte",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        public ResponseEntity<byte[]> descargarReporteConciliacion(
            @RequestParam(value = "anio", required = false) Integer anio,
            @RequestParam(value = "mes", required = false) Integer mes) {
        try {
            ConsolidacionConciliacionReportService.GeneratedConciliacionReport report =
                consolidacionConciliacionReportService.generar(anio, mes);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + report.fileName() + "\"")
                .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(report.content());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(NOT_FOUND, ex.getMessage());
        }
        }

    /**
     * Ejecuta la consolidación manual de un periodo. Restringido a administradores
     * (grupo {@code A_SIPRO_Admin_Permisos}). No valida ventana de carga ni tiempos.
     *
     * @param periodo     Periodo de valoración en formato yyyy-MM-dd (ej: 2026-02-28)
     * @param observacion Texto libre de justificación (opcional)
     */
    @PostMapping(value = "/consolidacion/manual", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<ConsolidacionManualStatusResponse> consolidarManual(
            @AuthenticationPrincipal SiproAuthenticatedUser user,
            @RequestParam("periodo") String periodo,
            @RequestParam(value = "observacion", required = false) String observacion) {
        Long idUsuario = requireAuthenticatedUserId(user);
        requireAdminRole(user);

        LocalDate periodoValoracion;
        try {
            periodoValoracion = LocalDate.parse(periodo);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(crearRespuestaInvalida(periodo, "Formato de periodo inválido. Use yyyy-MM-dd."));
        }

        try {
            ConsolidacionManualStatusResponse response = consolidacionManualAsyncService
            .iniciar(periodoValoracion, idUsuario,
                observacion != null && !observacion.isBlank()
                    ? observacion.trim()
                    : "Consolidación manual forzada");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error en consolidación manual para periodo {}: {}", periodoValoracion, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(crearRespuestaInvalida(periodoValoracion.toString(), "Error en consolidación: " + e.getMessage()));
        }
    }

    /**
     * Consulta el estado guardado de una consolidación manual por periodo.
     */
    @GetMapping(value = "/consolidacion/manual/estado", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<ConsolidacionManualStatusResponse> obtenerEstadoConsolidacionManual(
            @RequestParam("periodo") String periodo) {
        LocalDate periodoValoracion;
        try {
            periodoValoracion = LocalDate.parse(periodo);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(crearRespuestaInvalida(periodo, "Formato de periodo inválido. Use yyyy-MM-dd."));
        }

        return consolidacionManualAsyncService.obtenerEstado(periodoValoracion)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(crearRespuestaSinEjecucion(periodoValoracion.toString())));
    }

    private ConsolidacionManualStatusResponse crearRespuestaInvalida(String periodo, String mensaje) {
        ConsolidacionManualStatusResponse response = new ConsolidacionManualStatusResponse();
        response.setPeriodo(periodo);
        response.setEstado("NO_EJECUTADA");
        response.setMensaje(mensaje);
        response.setTerminal(true);
        response.setExito(false);
        response.setCantidadArchivosConsolidados(0);
        response.setCantidadRegistrosConsolidados(0);
        response.setMensajeError(mensaje);
        return response;
    }

    private ConsolidacionManualStatusResponse crearRespuestaSinEjecucion(String periodo) {
        ConsolidacionManualStatusResponse response = new ConsolidacionManualStatusResponse();
        response.setPeriodo(periodo);
        response.setEstado("SIN_EJECUCION");
        response.setMensaje("No hay una consolidación registrada para ese periodo todavía.");
        response.setTerminal(true);
        response.setExito(false);
        response.setCantidadArchivosConsolidados(0);
        response.setCantidadRegistrosConsolidados(0);
        return response;
    }

    private Long requireAuthenticatedUserId(SiproAuthenticatedUser user) {
        if (user == null || user.idUsuario() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "No se pudo resolver el usuario autenticado.");
        }
        return user.idUsuario();
    }

    private void requireAdminRole(SiproAuthenticatedUser user) {
        if (user == null || user.groupNames() == null
                || !user.groupNames().contains("A_SIPRO_Admin_Permisos")) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Solo el administrador (A_SIPRO_Admin_Permisos) puede ejecutar consolidaciones manuales.");
        }
    }
}
