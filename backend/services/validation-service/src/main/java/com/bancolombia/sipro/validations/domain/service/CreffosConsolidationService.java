package com.bancolombia.sipro.validations.domain.service;

import com.bancolombia.sipro.validations.domain.model.SiproDetalleConsolidadoRegistro;
import com.bancolombia.sipro.validations.infrastructure.repository.SiproDetalleConsolidadoRegistroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera y publica el archivo CREFFSOS a partir de los registros consolidados del periodo.
 */
@Service
public class CreffosConsolidationService {

    private static final Logger logger = LoggerFactory.getLogger(CreffosConsolidationService.class);
    private static final String CONSOLIDADOS_PREFIX = "consolidados/";
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SiproDetalleConsolidadoRegistroRepository consolidadoRegistroRepository;
    private final CreffosParametricGenerator creffosParametricGenerator;
    private final FileStorageService fileStorageService;
    private final ParametroUnicoService parametroUnicoService;

    public CreffosConsolidationService(SiproDetalleConsolidadoRegistroRepository consolidadoRegistroRepository,
                                       CreffosParametricGenerator creffosParametricGenerator,
                                       FileStorageService fileStorageService,
                                       ParametroUnicoService parametroUnicoService) {
        this.consolidadoRegistroRepository = consolidadoRegistroRepository;
        this.creffosParametricGenerator = creffosParametricGenerator;
        this.fileStorageService = fileStorageService;
        this.parametroUnicoService = parametroUnicoService;
    }

    /**
     * Mantiene la firma incremental, pero hoy delega a una reconstrucción completa del archivo.
     */
    @Async
    public void consolidarIncremental(LocalDate fechaCorte, String rutaExcelAprobado, String nombreArchivo) {
        logger.info("[CREFFSOS] Consolidación incremental delegada a reconstrucción paramétrica para fecha={}, archivo={}",
                fechaCorte, nombreArchivo);
        reconstruirCompleto(fechaCorte);
    }

    /**
     * Regenera el archivo CREFFSOS completo y lo publica en storage y, si aplica, en la ruta compartida.
     */
    public PublicationResult reconstruirCompleto(LocalDate fechaCorte) {
        List<SiproDetalleConsolidadoRegistro> registros = consolidadoRegistroRepository
                .findByFechaCorteOrderByIdConsolidadoRegistroAsc(fechaCorte);

        if (registros.isEmpty()) {
            logger.warn("[CREFFSOS] No hay registros consolidados para fecha={}. No se genera archivo.", fechaCorte);
            return PublicationResult.notGenerated();
        }

        CreffosParametricGenerator.GeneratedCreffosFile generatedFile =
                creffosParametricGenerator.generate(fechaCorte, registros);

        String fechaStr = fechaCorte.format(FECHA_FMT);
        String storageKey = CONSOLIDADOS_PREFIX + fechaStr + "/" + generatedFile.fileName();

        try {
            fileStorageService.storeBytes(generatedFile.content(), storageKey, generatedFile.contentType());
            String sharedCopyWarning = publicarEnRutaCompartida(generatedFile);
            logger.info("[CREFFSOS] Archivo paramétrico generado: {} (filas={}, formato={})",
                    storageKey, generatedFile.rowCount(), generatedFile.format());
            return PublicationResult.generated(storageKey, generatedFile.fileName(), sharedCopyWarning);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo publicar el archivo CREFFSOS paramétrico: " + ex.getMessage(), ex);
        }
    }

    private String publicarEnRutaCompartida(CreffosParametricGenerator.GeneratedCreffosFile generatedFile) {
        String outputDir = parametroUnicoService.getString("CREFFSOS_RUTA_SALIDA", "");
        if (outputDir == null || outputDir.isBlank()) {
            return null;
        }

        try {
            Path targetDir = Path.of(outputDir.trim());
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(generatedFile.fileName());
            Files.write(targetFile,
                    generatedFile.content(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            logger.info("[CREFFSOS] Archivo publicado en ruta compartida: {}", targetFile);
            return null;
        } catch (Exception ex) {
            logger.warn("No se pudo copiar CREFFSOS a ruta compartida: {}. Motivo: {}",
                    outputDir.trim(), ex.getMessage());
            return "No se pudo copiar CREFFSOS a ruta compartida: " + outputDir.trim() + ". Motivo: " + ex.getMessage();
        }
    }

    public record PublicationResult(boolean generated,
                                    String storageKey,
                                    String fileName,
                                    String sharedCopyWarning) {

        public static PublicationResult generated(String storageKey, String fileName, String sharedCopyWarning) {
            return new PublicationResult(true, storageKey, fileName, sharedCopyWarning);
        }

        public static PublicationResult notGenerated() {
            return new PublicationResult(false, null, null, null);
        }
    }
}