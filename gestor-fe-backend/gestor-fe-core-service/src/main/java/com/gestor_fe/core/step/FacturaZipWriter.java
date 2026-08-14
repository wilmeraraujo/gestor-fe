package com.gestor_fe.core.step;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.repository.DocumentoRepository;
import com.gestor_fe.core.repository.FacturaRepository;

public class FacturaZipWriter implements ItemWriter<Factura> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacturaZipWriter.class);

    private final FacturaRepository facturaRepository;
    private final DocumentoRepository documentoRepository;
    private final String rutaStorageValidos;
    private final Long identificadorCargue;

    public FacturaZipWriter(FacturaRepository facturaRepository, 
                            DocumentoRepository documentoRepository,
                            String rutaStorageValidos, 
                            Long identificadorCargue) {
        this.facturaRepository = facturaRepository;
        this.documentoRepository = documentoRepository;
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
            
            String nitCarpeta = sanearNombreCarpeta(factura.getNit());
            String numFacturaCarpeta = sanearNombreCarpeta(factura.getNumeroFactura());

            // Estructura de almacenamiento: E:\gestion-fe-validos\NIT\NUM_FACTURA\
            Path directorioFactura = Paths.get(rutaStorageValidos, nitCarpeta, numFacturaCarpeta);
            
            if (!Files.exists(directorioFactura)) {
                Files.createDirectories(directorioFactura);
            }

            // 1. Mover y guardar los documentos provenientes del ZIP (XML y PDF propios de la Factura)
            if (factura.getDocumentos() != null) {
                for (Documento doc : factura.getDocumentos()) {
                    if (doc.getArchivoTemporal() != null) {
                        File archivoTemporal = doc.getArchivoTemporal();
                        String nombreUnico = UUID.randomUUID() + "_" + doc.getNombreOriginal();
                        Path destinoFinal = directorioFactura.resolve(nombreUnico);
                        
                        Files.move(archivoTemporal.toPath(), destinoFinal, StandardCopyOption.REPLACE_EXISTING);
                        doc.setRuta(destinoFinal.toString());
                    }
                }
            }

            // =========================================================================================
            // 📸 2. SNAPSHOT (CONGELAMIENTO) DINÁMICO DE TODOS LOS SOPORTES ACTIVOS DEL PRESTADOR
            // =========================================================================================
            // Consulta todos los documentos del prestador que no estén borrados (deletedAt IS NULL)
            List<Documento> soportesPrestador = documentoRepository.findSoportesPrestadorByNit(factura.getNit());

            for (Documento soporteOrigen : soportesPrestador) {
                // Verificamos que el soporte esté activo y tenga ruta física configurada
                if (soporteOrigen.getDeletedAt() == null && soporteOrigen.getRuta() != null) {
                    File archivoOrigen = new File(soporteOrigen.getRuta());
                    
                    if (archivoOrigen.exists()) {
                        String nombreCopia = "SOPORTE_" + soporteOrigen.getTipoId() + "_" + UUID.randomUUID() + "_" + soporteOrigen.getNombreOriginal();
                        Path destinoCopia = directorioFactura.resolve(nombreCopia);

                        // Copiar físicamente el archivo del prestador a la carpeta única de esta factura
                        Files.copy(archivoOrigen.toPath(), destinoCopia, StandardCopyOption.REPLACE_EXISTING);

                        // Crear y asociar el registro del soporte de forma inmutable a esta Factura
                        Documento docCopia = new Documento();
                        docCopia.setNombreOriginal(soporteOrigen.getNombreOriginal());
                        docCopia.setRuta(destinoCopia.toString());
                        docCopia.setTamano(soporteOrigen.getTamano());
                        docCopia.setEstadoId(1L);
                        docCopia.setExtensionId(soporteOrigen.getExtensionId());
                        docCopia.setTipoId(soporteOrigen.getTipoId());
                        
                        factura.addDocumento(docCopia);
                    } else {
                        LOGGER.warn("⚠️ El archivo físico del soporte ID [{}] en la ruta [{}] no fue encontrado en disco.", 
                                soporteOrigen.getId(), soporteOrigen.getRuta());
                    }
                }
            }
        }

        // Guardado atómico de la factura y todos sus documentos asociados (factura + soportes del prestador)
        facturaRepository.saveAll(chunk.getItems());
        LOGGER.info("=== ✅ Bloque de facturas y sus expedientes congelados de soportes guardados exitosamente ===");
    }

    private String sanearNombreCarpeta(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "DESCONOCIDO";
        }
        return nombre.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}