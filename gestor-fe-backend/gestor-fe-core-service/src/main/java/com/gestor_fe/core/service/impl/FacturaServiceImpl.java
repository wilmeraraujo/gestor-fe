package com.gestor_fe.core.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.dto.GestionDto;
import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.repository.FacturaRepository;
import com.gestor_fe.core.service.FacturaService;

@Service
public class FacturaServiceImpl implements FacturaService {

    private final FacturaRepository repository;

    @Value("${ruta.storage.validos}")
    private String rutaStorageValidos;

    public FacturaServiceImpl(FacturaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> findByNitAndDeletedAtIsNull(String nit, Pageable pageable) {
        return repository.findByNitAndDeletedAtIsNull(nit, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> findByFaseIdAndDeletedAtIsNull(Long faseId, Pageable pageable) {
        return repository.findByFaseIdAndDeletedAtIsNull(faseId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> findByFaseActiva(Long faseId, Pageable pageable) {
        return repository.findByFaseActiva(faseId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    // =========================================================================
    // ⚙️ MOTOR UNIFICADO DE TRANSICIÓN DE FASES (DSO / TEXTO)
    // =========================================================================
    @Override
    @Transactional
    public Factura procesarTransicionFase(Long id, Long faseActualId, GestionDto dto) {
        Factura factura = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con el ID: " + id));

        boolean esAprobado = "APROBADO".equalsIgnoreCase(dto.getEstadoAccion());

        // Evaluar reglas de negocio según la etapa actual
        int fase = faseActualId != null ? faseActualId.intValue() : 1;

        switch (fase) {
            case 1: // 📝 ETAPA 1: GESTIÓN INICIAL
                if (esAprobado) {
                    factura.setEstado("EN GESTIÓN");
                    factura.setFaseId(2L); // Avanza a Reconocimiento Contable
                    factura.setObservacion(null);
                    factura.setCausalDevolucionId(null);
                } else {
                    factura.setEstado("ANULADO");
                    factura.setFaseId(1L); // Se mantiene en Fase 1
                    factura.setCausalDevolucionId(dto.getCausalDevolucionId());
                    factura.setObservacion(dto.getObservacion());
                }
                break;

            case 2: // 🏦 ETAPA 2: RECONOCIMIENTO CONTABLE (Flujo alterno sin archivo)
                if (esAprobado) {
                    factura.setEstado("CAUSADO");
                    factura.setTipoRegistroContable(dto.getTipoRegistroContable()); // FC, GV, ORC, NI
                    factura.setNumeroCausacion(dto.getNumeroCausacion());
                    factura.setFaseId(3L); // Avanza a Impuestos
                    factura.setObservacion(null);
                    factura.setCausalDevolucionId(null);
                } else {
                    factura.setEstado("ANULADO");
                    factura.setCausalDevolucionId(dto.getCausalDevolucionId());
                    factura.setObservacion(dto.getObservacion());
                }
                break;

            case 3: // 📑 ETAPA 3: IMPUESTOS
                if (esAprobado) {
                    factura.setEstado("IMPUESTOS VERIFICADOS");
                    factura.setFaseId(4L); // Avanza a Pendiente de Pago - Tesorería
                    factura.setObservacion(null);
                    factura.setCausalDevolucionId(null);
                } else {
                    factura.setEstado("ANULADO");
                    factura.setCausalDevolucionId(dto.getCausalDevolucionId());
                    factura.setObservacion(dto.getObservacion());
                }
                break;

            case 4: // 💸 ETAPA 4: PENDIENTE DE PAGO - TESORERÍA (Flujo alterno sin archivo)
                if (esAprobado) {
                    factura.setEstado("PAGADO");
                    factura.setTipoRegistroContable("TB"); // Transferencia Bancaria
                    factura.setFaseId(4L); // Queda finalizada en Fase 4
                    factura.setObservacion(null);
                    factura.setCausalDevolucionId(null);
                } else {
                    factura.setEstado("ANULADO");
                    factura.setCausalDevolucionId(dto.getCausalDevolucionId());
                    factura.setObservacion(dto.getObservacion());
                }
                break;

            default:
                throw new IllegalArgumentException("La fase proporcionada no es válida: " + faseActualId);
        }

        return repository.save(factura);
    }

    // =========================================================================
    // 🏦 ETAPA 2: PROCESO DE CAUSACIÓN CON ARCHIVO MULTIPART (PDF)
    // =========================================================================
    @Override
    @Transactional
    public Factura procesarCausacionFase2(Long id, String tipoRegistroContable, String numeroCausacion, MultipartFile archivoCausacion) {
        Factura factura = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con el ID: " + id));

        // 1. Actualizar estado y promover a Fase 3 (Impuestos)
        factura.setEstado("CAUSADO");
        factura.setTipoRegistroContable(tipoRegistroContable);
        factura.setNumeroCausacion(numeroCausacion);
        factura.setFaseId(3L);
        factura.setObservacion(null);
        factura.setCausalDevolucionId(null);

        // 2. Procesar y guardar el archivo si llegó en la petición
        if (archivoCausacion != null && !archivoCausacion.isEmpty()) {
            try {
                String nitCarpeta = factura.getNit().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                String numFacturaCarpeta = factura.getNumeroFactura().replaceAll("[\\\\/:*?\"<>|]", "_").trim();

                Path directorioFactura = Paths.get(rutaStorageValidos, nitCarpeta, numFacturaCarpeta);
                if (!Files.exists(directorioFactura)) {
                    Files.createDirectories(directorioFactura);
                }

                String nombreOriginal = archivoCausacion.getOriginalFilename();
                String nombreUnico = UUID.randomUUID() + "_causacion_" + nombreOriginal;
                Path destinoFinal = directorioFactura.resolve(nombreUnico);

                // Guardado físico en disco
                archivoCausacion.transferTo(destinoFinal.toFile());

                // Mapeo del nuevo documento
                Documento docCausacion = new Documento();
                docCausacion.setNombreOriginal(nombreOriginal);
                docCausacion.setRuta(destinoFinal.toString());
                docCausacion.setTamano(archivoCausacion.getSize());
                docCausacion.setEstadoId(1L);
                docCausacion.setExtensionId(1L); // 1 = PDF
                docCausacion.setTipoId(8L);      // 8 = PDF Soporte de Causación
                docCausacion.setFactura(factura);

                // Agregar a la colección asociativa
                factura.addDocumento(docCausacion);

            } catch (IOException e) {
                throw new RuntimeException("Error al guardar físicamente el PDF de causación: " + e.getMessage(), e);
            }
        }

        return repository.save(factura);
    }

    // =========================================================================
    // 💸 ETAPA 4: PROCESO DE REGISTRO DE PAGO CON SOPORTES (TB + COMPROBANTE)
    // =========================================================================
    @Override
    @Transactional
    public Factura procesarPagoFase4(Long id, String numeroCausacion, MultipartFile soporteTb, MultipartFile comprobantePago) {
        Factura factura = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con el ID: " + id));

        // 1. Actualizar estado final a PAGADO y asociar referencia de egreso/causación si aplica
        factura.setEstado("PAGADO");
        factura.setTipoRegistroContable("TB"); // Transferencia Bancaria
        if (numeroCausacion != null && !numeroCausacion.isBlank()) {
            factura.setNumeroCausacion(numeroCausacion);
        }
        factura.setFaseId(4L); // Permanece finalizada en Fase 4
        factura.setObservacion(null);
        factura.setCausalDevolucionId(null);

        String nitCarpeta = factura.getNit().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        String numFacturaCarpeta = factura.getNumeroFactura().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        Path directorioFactura = Paths.get(rutaStorageValidos, nitCarpeta, numFacturaCarpeta);

        try {
            if (!Files.exists(directorioFactura)) {
                Files.createDirectories(directorioFactura);
            }

            // 2. Guardar Documento TB si fue adjuntado
            if (soporteTb != null && !soporteTb.isEmpty()) {
                guardarSoporteDocumento(factura, soporteTb, directorioFactura, "TB_", 8L);
            }

            // 3. Guardar Comprobante de Pago Bancario si fue adjuntado
            if (comprobantePago != null && !comprobantePago.isEmpty()) {
                guardarSoporteDocumento(factura, comprobantePago, directorioFactura, "PAGO_", 8L);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar los soportes de pago en disco: " + e.getMessage(), e);
        }

        return repository.save(factura);
    }

    /**
     * 📁 Método privado reutilizable para abstraer el guardado físico y relacional de soportes PDF
     */
    private void guardarSoporteDocumento(Factura factura, MultipartFile archivo, Path directorio, String prefijo, Long tipoId) throws IOException {
        String nombreOriginal = archivo.getOriginalFilename();
        String nombreUnico = UUID.randomUUID() + "_" + prefijo + nombreOriginal;
        Path destinoFinal = directorio.resolve(nombreUnico);

        archivo.transferTo(destinoFinal.toFile());

        Documento doc = new Documento();
        doc.setNombreOriginal(nombreOriginal);
        doc.setRuta(destinoFinal.toString());
        doc.setTamano(archivo.getSize());
        doc.setEstadoId(1L);
        doc.setExtensionId(1L); // 1 = PDF
        doc.setTipoId(tipoId);
        doc.setFactura(factura);

        factura.addDocumento(doc);
    }

    // =========================================================================
    // 🔍 VALIDACIONES Y CONSULTAS SECUNDARIAS
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<String> findExistingCufes(List<String> cufes) {
        if (cufes == null || cufes.isEmpty()) {
            return List.of();
        }
        return repository.findExistingCufes(cufes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findExistingNitFacturas(List<String> nitFacturas) {
        if (nitFacturas == null || nitFacturas.isEmpty()) {
            return List.of();
        }
        return repository.findExistingNitFacturas(nitFacturas);
    }
}