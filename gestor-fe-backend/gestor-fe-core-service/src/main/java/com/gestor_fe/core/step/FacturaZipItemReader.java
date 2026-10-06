package com.gestor_fe.core.step;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.gestor_fe.core.dto.FacturaZipWrapperDto;
import com.gestor_fe.core.entity.ErrorCargue;
import com.gestor_fe.core.repository.ErrorCargueRepository;

public class FacturaZipItemReader implements ItemReader<FacturaZipWrapperDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacturaZipItemReader.class);

    private final String rutaZip;
    private final ErrorCargueRepository errorCargueRepository;
    private final PlatformTransactionManager transactionManager; // 👈 Manejador explícito de transacciones
    private final Long cargueId;
    
    private Iterator<FacturaZipWrapperDto> iterator;
    private boolean procesado = false;

    public FacturaZipItemReader(String rutaZip, 
                               ErrorCargueRepository errorCargueRepository, 
                               PlatformTransactionManager transactionManager,
                               Long cargueId) {
        this.rutaZip = rutaZip;
        this.errorCargueRepository = errorCargueRepository;
        this.transactionManager = transactionManager;
        this.cargueId = cargueId;
    }

    @Override
    public FacturaZipWrapperDto read() throws Exception {
        if (!procesado) {
            LOGGER.info("=== 📂 Iniciando descompresión y validación estricta del ZIP: {} ===", rutaZip);
            List<FacturaZipWrapperDto> listaEmparejada = descomprimirYEmparejar();
            this.iterator = listaEmparejada.iterator();
            this.procesado = true;
        }

        if (iterator != null && iterator.hasNext()) {
            return iterator.next();
        }
        return null; 
    }

    private List<FacturaZipWrapperDto> descomprimirYEmparejar() throws Exception {
        File zipFileObj = new File(rutaZip);
        if (!zipFileObj.exists()) {
            throw new IllegalArgumentException("El archivo ZIP especificado no existe en el disco.");
        }

        File dirTemporal = Files.createTempDirectory("gestorfe-batch-zip-").toFile();
        dirTemporal.deleteOnExit();
        
        Map<String, File> mapasXml = new HashMap<>();
        Map<String, File> mapasPdf = new HashMap<>();
        
        Map<String, String> nombresOriginalesXml = new HashMap<>();
        Map<String, String> nombresOriginalesPdf = new HashMap<>();

        List<ErrorCargue> listaErrores = new ArrayList<>();
        int lineaError = 1;

        try (ZipFile zipFile = new ZipFile(zipFileObj)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                
                if (entry.isDirectory() || entry.getName().contains("__MACOSX")) {
                    continue;
                }

                String nombreArchivoCompleto = entry.getName();
                String nombreArchivo = new File(nombreArchivoCompleto).getName();
                String extension = StringUtils.getFilenameExtension(nombreArchivo);
                String nombreBase = StringUtils.stripFilenameExtension(nombreArchivo);

                if (extension == null) continue;

                // 🛑 VALIDACIÓN ESTRICTA DE FORMATOS DENTRO DEL ZIP
                if (!extension.equalsIgnoreCase("xml") && !extension.equalsIgnoreCase("pdf")) {
                    String errorMsg = String.format("Existen archivos con extensión no permitida en el ZIP. Archivo no válido: [%s]", nombreArchivo);
                    
                    LOGGER.error("❌ ERROR DE EXTENSIÓN EN ZIP: {}", errorMsg);

                    ErrorCargue error = new ErrorCargue();
                    error.setCargueId(cargueId);
                    error.setNumeroLinea(lineaError++);
                    error.setTipoError("EXTENSION_NO_PERMITIDA");
                    error.setCampo("ARCHIVO");
                    error.setError(errorMsg);
                    error.setValorAsociado(nombreArchivo);
                    error.setCreatedAt(LocalDateTime.now());
                    listaErrores.add(error);

                    continue; 
                }

                File archivoExtraido = new File(dirTemporal, nombreArchivo);

                try (InputStream is = zipFile.getInputStream(entry);
                     FileOutputStream fos = new FileOutputStream(archivoExtraido)) {
                    byte[] buffer = new byte[4096];
                    int bytesLeidos;
                    while ((bytesLeidos = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesLeidos);
                    }
                }

                String raizUnica = nombreBase;
                if (nombreBase.startsWith("ad") || nombreBase.startsWith("de")) {
                    raizUnica = nombreBase.substring(2);
                }

                if (extension.equalsIgnoreCase("xml")) {
                    mapasXml.put(raizUnica, archivoExtraido);
                    nombresOriginalesXml.put(raizUnica, nombreArchivo);
                } else if (extension.equalsIgnoreCase("pdf")) {
                    mapasPdf.put(raizUnica, archivoExtraido);
                    nombresOriginalesPdf.put(raizUnica, nombreArchivo);
                }
            }
        }

        // ⚡ SI EXISTEN ARCHIVOS NO PERMITIDOS: Guardado en Transacción Independiente (REQUIRES_NEW)
        if (!listaErrores.isEmpty()) {
            guardarErroresEnTransaccionIndependiente(listaErrores);
            throw new IllegalStateException("El cargue fue rechazado debido a que existen " + listaErrores.size() + " archivo(s) con extensión no permitida.");
        }

        List<FacturaZipWrapperDto> listaWrappers = new ArrayList<>();

        // === REGLA DE VALIDACIÓN ESTRICTA: TODO O NADA (PAREJAS HUÉRFANAS) ===
        
        // 1. Validar XMLs sin PDF
        for (String raiz : mapasXml.keySet()) {
            File xml = mapasXml.get(raiz);
            File pdf = mapasPdf.get(raiz);

            if (pdf == null) {
                String xmlNombre = nombresOriginalesXml.get(raiz);
                String errorMsg = String.format("El archivo XML [%s] no tiene su archivo PDF correspondiente en el ZIP.", xmlNombre);
                LOGGER.error("❌ ARCHIVO HUÉRFANO: {}", errorMsg);
                
                ErrorCargue error = new ErrorCargue();
                error.setCargueId(cargueId);
                error.setNumeroLinea(lineaError++);
                error.setTipoError("ARCHIVO_HUERFANO");
                error.setCampo("PDF");
                error.setError(errorMsg);
                error.setValorAsociado(xmlNombre);
                error.setCreatedAt(LocalDateTime.now());
                listaErrores.add(error);
            } else {
                listaWrappers.add(FacturaZipWrapperDto.builder()
                        .nombreBase(raiz)
                        .archivoXml(xml)
                        .archivoPdf(pdf)
                        .build());
            }
        }

        // 2. Validar PDFs sin XML
        for (String raiz : mapasPdf.keySet()) {
            if (!mapasXml.containsKey(raiz)) {
                String pdfNombre = nombresOriginalesPdf.get(raiz);
                String errorMsg = String.format("El archivo PDF [%s] no tiene su archivo XML correspondiente en el ZIP.", pdfNombre);
                LOGGER.error("❌ ARCHIVO HUÉRFANO: {}", errorMsg);
                
                ErrorCargue error = new ErrorCargue();
                error.setCargueId(cargueId);
                error.setNumeroLinea(lineaError++);
                error.setTipoError("ARCHIVO_HUERFANO");
                error.setCampo("XML");
                error.setError(errorMsg);
                error.setValorAsociado(pdfNombre);
                error.setCreatedAt(LocalDateTime.now());
                listaErrores.add(error);
            }
        }

        // ⚡ SI HUBO ERRORES DE PAREJA: Guardado en Transacción Independiente
        if (!listaErrores.isEmpty()) {
            guardarErroresEnTransaccionIndependiente(listaErrores);
            throw new IllegalStateException("Se canceló el cargue masivo debido a que " + listaErrores.size() + " archivo(s) fallaron la regla de pares XML/PDF.");
        }

        return listaWrappers;
    }

    /**
     * 🛡️ Ejecuta un COMMIT físico e independiente en PostgreSQL (PROPAGATION_REQUIRES_NEW)
     * para aislar los errores del Rollback automático de Spring Batch.
     */
    private void guardarErroresEnTransaccionIndependiente(List<ErrorCargue> errores) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transactionTemplate.executeWithoutResult(status -> {
            errorCargueRepository.saveAllAndFlush(errores);
            LOGGER.info("💾 Transacción aislada completada: Persistidos {} errores en gestor.error_cargue", errores.size());
        });
    }
}