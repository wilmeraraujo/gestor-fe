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
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.util.StringUtils;

import com.gestor_fe.core.dto.FacturaZipWrapperDto;
import com.gestor_fe.core.entity.ErrorCargue;
import com.gestor_fe.core.service.ErrorCargueService; // <-- CORREGIDO: Importamos el servicio

public class FacturaZipItemReader implements ItemReader<FacturaZipWrapperDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacturaZipItemReader.class);

    private final String rutaZip;
    private final ErrorCargueService errorCargueService; // <-- CORREGIDO: Cambiado de Repository a Service
    private final Long cargueId;
    
    private Iterator<FacturaZipWrapperDto> iterator;
    private boolean procesado = false;

    // CORREGIDO: El constructor ahora recibe el ErrorCargueService para aislar la transacción
    public FacturaZipItemReader(String rutaZip, ErrorCargueService errorCargueService, Long cargueId) {
        this.rutaZip = rutaZip;
        this.errorCargueService = errorCargueService;
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
            throw new IllegalArgumentException("El archivo ZIP no existe.");
        }

        File dirTemporal = Files.createTempDirectory("gestorfe-batch-zip-").toFile();
        dirTemporal.deleteOnExit();
        
        Map<String, File> mapasXml = new HashMap<>();
        Map<String, File> mapasPdf = new HashMap<>();
        
        Map<String, String> nombresOriginalesXml = new HashMap<>();
        Map<String, String> nombresOriginalesPdf = new HashMap<>();

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

        List<FacturaZipWrapperDto> listaWrappers = new ArrayList<>();
        List<ErrorCargue> listaErrores = new ArrayList<>(); 

        // === REGLA DE VALIDACIÓN ESTRICTA: TODO O NADA ===
        
        // 1. Validar XMLs huérfanos sin su PDF
        int lineaError = 1;
        for (String raiz : mapasXml.keySet()) {
            File xml = mapasXml.get(raiz);
            File pdf = mapasPdf.get(raiz);

            if (pdf == null) {
                String xmlNombre = nombresOriginalesXml.get(raiz);
                String errorMsg = String.format("El archivo XML [%s] no tiene el archivo PDF correspondiente en el ZIP.", xmlNombre);
                LOGGER.error("❌ ERROR CRÍTICO: {}", errorMsg);
                
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

        // 2. Validar PDFs huérfanos sin su XML
        for (String raiz : mapasPdf.keySet()) {
            if (!mapasXml.containsKey(raiz)) {
                String pdfNombre = nombresOriginalesPdf.get(raiz);
                String errorMsg = String.format("El archivo PDF [%s] no tiene el archivo XML correspondiente en el ZIP.", pdfNombre);
                LOGGER.error("❌ ERROR CRÍTICO: {}", errorMsg);
                
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

        // Si se capturó algún error, guardamos el listado y detenemos la transacción
        if (!listaErrores.isEmpty()) {
            // CORREGIDO: Invocamos al servicio con Propagation.REQUIRES_NEW para salvar los registros del rollback
            errorCargueService.saveAll(listaErrores); 
            
            String failMsg = "Se canceló el procesamiento del lote debido a que " + listaErrores.size() + " archivos fallaron la regla de parejas (Todo o Nada).";
            throw new IllegalStateException(failMsg); 
        }

        return listaWrappers;
    }
}