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
    private final String rutaStorage;
    private final Long identificadorCargue;

    public FacturaZipWriter(FacturaRepository facturaRepository, String rutaStorage, Long identificadorCargue) {
        this.facturaRepository = facturaRepository;
        this.rutaStorage = rutaStorage;
        this.identificadorCargue = identificadorCargue;
    }

    @Override
    public void write(Chunk<? extends Factura> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        LOGGER.info("=== 💾 Procesando e ingresando un bloque de {} facturas para el cargue {} ===", chunk.size(), identificadorCargue);

        // Verificar o crear el directorio definitivo en el servidor de archivos
        File carpetaDestino = new File(rutaStorage);
        if (!carpetaDestino.exists()) {
            carpetaDestino.mkdirs();
        }

        for (Factura factura : chunk.getItems()) {
            
            // Iterar dinámicamente sobre todos los documentos asociados (Relación Uno a Muchos)
            if (factura.getDocumentos() != null) {
                for (Documento doc : factura.getDocumentos()) {
                    
                    if (doc.getArchivoTemporal() != null) {
                        File archivoTemporal = doc.getArchivoTemporal();
                        
                        // Generar nombre UUID único para evitar colisiones en la carpeta de destino
                        String nombreUnico = UUID.randomUUID() + "_" + doc.getNombreOriginal();
                        Path destinoFinal = Paths.get(rutaStorage, nombreUnico);
                        
                        // Mover archivo físico desde la carpeta transitoria del ZIP al almacenamiento definitivo
                        Files.move(archivoTemporal.toPath(), destinoFinal, StandardCopyOption.REPLACE_EXISTING);
                        
                        // Inyectar la ubicación real absoluta que irá en la tabla gestor.documento
                        doc.setRuta(destinoFinal.toString());
                    }
                }
            }
        }

        // Persistencia relacional de todo el Chunk y sus hijos asociados en cascada de forma atómica
        facturaRepository.saveAll(chunk.getItems());
        LOGGER.info("=== ✅ Bloque de facturas y sus múltiples documentos guardados en PostgreSQL ===");
    }
}