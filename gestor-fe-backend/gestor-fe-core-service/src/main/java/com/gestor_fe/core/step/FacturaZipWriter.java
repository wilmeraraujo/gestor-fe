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
            
            // 1. Transferencia física y cambio de nombre del archivo XML
            if (factura.getDocumentoXml() != null && factura.getDocumentoXml().getArchivoTemporal() != null) {
                File xmlTemporal = factura.getDocumentoXml().getArchivoTemporal();
                
                // Generar nombre UUID único para evitar colisiones en la carpeta compartida o volumen
                String nombreUnicoXml = UUID.randomUUID() + "_" + factura.getDocumentoXml().getNombreOriginal();
                Path destinoXml = Paths.get(rutaStorage, nombreUnicoXml);
                
                // Mover archivo desde la carpeta transitoria del ZIP al almacenamiento definitivo
                Files.move(xmlTemporal.toPath(), destinoXml, StandardCopyOption.REPLACE_EXISTING);
                
                // Inyectar la ubicación real absoluta que irá en la tabla gestor.documento
                factura.getDocumentoXml().setRuta(destinoXml.toString());
            }

            // 2. Transferencia física y cambio de nombre del archivo PDF (Opcional)
            if (factura.getDocumentoPdf() != null && factura.getDocumentoPdf().getArchivoTemporal() != null) {
                File pdfTemporal = factura.getDocumentoPdf().getArchivoTemporal();
                
                String nombreUnicoPdf = UUID.randomUUID() + "_" + factura.getDocumentoPdf().getNombreOriginal();
                Path destinoPdf = Paths.get(rutaStorage, nombreUnicoPdf);
                
                Files.move(pdfTemporal.toPath(), destinoPdf, StandardCopyOption.REPLACE_EXISTING);
                
                // Inyectar la ubicación real absoluta que irá en la tabla gestor.documento
                factura.getDocumentoPdf().setRuta(destinoPdf.toString());
            }
        }

        // 3. Persistencia relacional de todo el Chunk de manera atómica (Spring se encarga de la transacción)
        facturaRepository.saveAll(chunk.getItems());
        LOGGER.info("=== ✅ Bloque de facturas y registros de auditoría guardados en PostgreSQL ===");
    }
}