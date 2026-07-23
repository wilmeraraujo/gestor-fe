package com.gestor_fe.core.step;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.repository.FacturaRepository;

public class FacturaZipWriter implements ItemWriter<Factura> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacturaZipWriter.class);

    private final FacturaRepository facturaRepository;
    private final String rutaStorageValidos; // 👈 Cambiado para apuntar al directorio de válidos
    private final Long identificadorCargue;

    public FacturaZipWriter(FacturaRepository facturaRepository, String rutaStorageValidos, Long identificadorCargue) {
        this.facturaRepository = facturaRepository;
        this.rutaStorageValidos = rutaStorageValidos;
        this.identificadorCargue = identificadorCargue;
    }

    @Override
    public void write(Chunk<? extends Factura> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        LOGGER.info("=== 💾 Procesando e ingresando un bloque de {} facturas para el cargue {} ===", chunk.size(), identificadorCargue);

        for (Factura factura : chunk.getItems()) {
            
            // 1. Limpieza de nombres de carpetas para evitar caracteres ilegales en Windows/Linux
            String nitCarpeta = sanearNombreCarpeta(factura.getNit());
            String numFacturaCarpeta = sanearNombreCarpeta(factura.getNumeroFactura());

            // 2. Construir la estructura de directorios: E:\gestion-fe-validos\NIT\NUM_FACTURA\
            Path directorioFactura = Paths.get(rutaStorageValidos, nitCarpeta, numFacturaCarpeta);
            
            // Crear carpetas padre e hijas si no existen
            if (!Files.exists(directorioFactura)) {
                Files.createDirectories(directorioFactura);
            }

            // 3. Iterar y mover los archivos PDF/XML asociados a esta factura
            if (factura.getDocumentos() != null) {
                for (Documento doc : factura.getDocumentos()) {
                    
                    if (doc.getArchivoTemporal() != null) {
                        File archivoTemporal = doc.getArchivoTemporal();
                        
                        // Mantenemos tu nombre único con UUID para evitar colisiones
                        String nombreUnico = UUID.randomUUID() + "_" + doc.getNombreOriginal();
                        
                        // Ruta final: E:\gestion-fe-validos\NIT\NUM_FACTURA\UUID_NombreOriginal.pdf
                        Path destinoFinal = directorioFactura.resolve(nombreUnico);
                        
                        // Mover archivo desde la carpeta transitoria al almacenamiento estructurado definitivo
                        Files.move(archivoTemporal.toPath(), destinoFinal, StandardCopyOption.REPLACE_EXISTING);
                        
                        // Guardar la ubicación absoluta estructurada en PostgreSQL
                        doc.setRuta(destinoFinal.toString());
                    }
                }
            }
        }

        // Persistencia atómica de las facturas y sus documentos en BD
        facturaRepository.saveAll(chunk.getItems());
        LOGGER.info("=== ✅ Bloque de facturas y sus múltiples documentos guardados exitosamente ===");
    }

    /**
     * Reemplaza caracteres no válidos en nombres de directorios (ej. /, \, :, *, ?, ", <, >, |)
     */
    private String sanearNombreCarpeta(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "DESCONOCIDO";
        }
        return nombre.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}