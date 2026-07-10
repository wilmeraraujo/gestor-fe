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
        // Inicialización diferida (Lazy): se ejecuta solo la primera vez que el paso llama al Reader
        if (!procesado) {
            LOGGER.info("=== 📂 Iniciando descompresión y emparejamiento del archivo ZIP: {} ===", rutaZip);
            List<FacturaZipWrapperDto> listaEmparejada = descomprimirYEmparejar();
            this.iterator = listaEmparejada.iterator();
            this.procesado = true;
            LOGGER.info("=== 🧩 Se detectaron y emparejaron un total de {} documentos para procesar ===", listaEmparejada.size());
        }

        // Retorna el siguiente elemento del lote
        if (iterator != null && iterator.hasNext()) {
            return iterator.next();
        }
        
        // Retornar 'null' es la señal que exige Spring Batch para saber que el Job ha terminado con éxito
        return null; 
    }

    private List<FacturaZipWrapperDto> descomprimirYEmparejar() throws Exception {
        File zipFileObj = new File(rutaZip);
        if (!zipFileObj.exists()) {
            throw new IllegalArgumentException("Error Crítico: El archivo ZIP no existe en la ruta especificada: " + rutaZip);
        }

        // Crear una carpeta temporal única en el sistema operativo para esta ejecución del lote
        File dirTemporal = Files.createTempDirectory("gestorfe-batch-zip-").toFile();
        
        // Mapas para almacenar temporalmente los archivos clasificados por su nombre sin extensión
        Map<String, File> mapasXml = new HashMap<>();
        Map<String, File> mapasPdf = new HashMap<>();

        try (ZipFile zipFile = new ZipFile(zipFileObj)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                
                // Ignorar carpetas internas o archivos ocultos del sistema (como los de MacOS __MACOSX)
                if (entry.isDirectory() || entry.getName().contains("__MACOSX")) {
                    continue;
                }

                String nombreArchivoCompleto = entry.getName();
                
                // Extraer el nombre plano si viene dentro de subcarpetas en el zip
                String nombreArchivo = new File(nombreArchivoCompleto).getName();
                
                String extension = StringUtils.getFilenameExtension(nombreArchivo);
                String nombreBase = StringUtils.stripFilenameExtension(nombreArchivo);

                if (extension == null) {
                    continue;
                }

                // Definir el destino físico del archivo transitorio
                File archivoExtraido = new File(dirTemporal, nombreArchivo);

                // Transferencia del stream del ZIP al disco temporal del servidor
                try (InputStream is = zipFile.getInputStream(entry);
                     FileOutputStream fos = new FileOutputStream(archivoExtraido)) {
                    byte[] buffer = new byte[4096];
                    int bytesLeidos;
                    while ((bytesLeidos = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesLeidos);
                    }
                }

                // Clasificación por tipo de documento para posterior emparejamiento
                if (extension.equalsIgnoreCase("xml")) {
                    mapasXml.put(nombreBase, archivoExtraido);
                } else if (extension.equalsIgnoreCase("pdf")) {
                    mapasPdf.put(nombreBase, archivoExtraido);
                }
            }
        }

        // Proceso de emparejamiento inteligente de estructuras XML + PDF
        List<FacturaZipWrapperDto> listaWrappers = new ArrayList<>();
        
        for (String nombreBase : mapasXml.keySet()) {
            File xml = mapasXml.get(nombreBase);
            File pdf = mapasPdf.get(nombreBase); // Puede ser null si la factura XML no cuenta con soporte PDF

            if (pdf == null) {
                LOGGER.warn("⚠️ Advertencia: La factura [{}] posee archivo XML pero no se encontró su PDF de soporte.", nombreBase);
            }

            listaWrappers.add(FacturaZipWrapperDto.builder()
                    .nombreBase(nombreBase)
                    .archivoXml(xml)
                    .archivoPdf(pdf)
                    .build());
        }

        return listaWrappers;
    }
}