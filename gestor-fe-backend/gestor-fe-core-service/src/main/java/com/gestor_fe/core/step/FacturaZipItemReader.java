package com.gestor_fe.core.step;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
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

public class FacturaZipItemReader implements ItemReader<FacturaZipWrapperDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacturaZipItemReader.class);

    private final String rutaZip;
    private Iterator<FacturaZipWrapperDto> iterator;
    private boolean procesado = false;

    public FacturaZipItemReader(String rutaZip) {
        this.rutaZip = rutaZip;
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
        
        // Guardaremos usando la parte numérica o identificador único (removiendo los prefijos de la DIAN)
        Map<String, File> mapasXml = new HashMap<>();
        Map<String, File> mapasPdf = new HashMap<>();
        
        // Guardar nombres originales para el reporte de error
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

                // Normalización de nombres de la DIAN: removemos "ad" o "de" si existen para extraer la raíz común
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

        // === REGLA DE VALIDACIÓN ESTRICTA: TODO O NADA ===
        
        // 1. Validar que cada XML tenga su PDF
        for (String raiz : mapasXml.keySet()) {
            File xml = mapasXml.get(raiz);
            File pdf = mapasPdf.get(raiz);

            if (pdf == null) {
                String errorMsg = String.format("❌ ERROR CRÍTICO [Todo o Nada]: El archivo XML [%s] no tiene su pareja PDF correspondiente en el ZIP.", nombresOriginalesXml.get(raiz));
                LOGGER.error(errorMsg);
                throw new IllegalStateException(errorMsg); // Rompe el Batch inmediatamente
            }

            listaWrappers.add(FacturaZipWrapperDto.builder()
                    .nombreBase(raiz)
                    .archivoXml(xml)
                    .archivoPdf(pdf)
                    .build());
        }

        // 2. Validar que no existan PDFs huérfanos sin su XML
        for (String raiz : mapasPdf.keySet()) {
            if (!mapasXml.containsKey(raiz)) {
                String errorMsg = String.format("❌ ERROR CRÍTICO [Todo o Nada]: El archivo PDF [%s] no tiene su pareja XML correspondiente en el ZIP.", nombresOriginalesPdf.get(raiz));
                LOGGER.error(errorMsg);
                throw new IllegalStateException(errorMsg); // Rompe el Batch inmediatamente
            }
        }

        return listaWrappers;
    }
}