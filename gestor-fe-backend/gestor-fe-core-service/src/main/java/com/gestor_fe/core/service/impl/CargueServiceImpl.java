package com.gestor_fe.core.service.impl;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.core.entity.Cargue;
import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.repository.CargueRepository;
import com.gestor_fe.core.repository.DocumentoRepository;
import com.gestor_fe.core.repository.FacturaRepository;
import com.gestor_fe.core.service.CargueService;

@Service
public class CargueServiceImpl implements CargueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CargueServiceImpl.class);

    @Autowired
    private CargueRepository cargueRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("procesarLoteFacturasJob")
    private Job procesarLoteFacturasJob;

    @Override
    @Transactional
    public Cargue save(Cargue cargue) {
        return cargueRepository.save(cargue);
    }

    @Override
    @Async("taskExecutor")
    public void runBatchJobAsynchronously(File fileToImport, Cargue cargue) {
        try {
            LOGGER.info("=== 🚀 Hilo secundario arrancando Job de Spring Batch de forma asíncrona ===");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("fullPathFileName", fileToImport.getAbsolutePath())
                    .addLong("identificadorCargue", cargue.getId())
                    .addString("nombreArchivo", cargue.getNombreArchivo())
                    .addString("usuario", cargue.getUsuario())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(procesarLoteFacturasJob, jobParameters);

        } catch (Exception e) {
            LOGGER.error("❌ Error crítico ejecutando el Job Asíncrono de Facturas: ", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Cargue> findByDeletedAtIsNull(Pageable pageable) {
        return cargueRepository.findByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Cargue> findCarguesSegunRol(String usuario, List<String> roles, Pageable pageable) {
        // Verificar si posee rol Administrador o Gestor de Cargue Global
        boolean esAdminOGestorGlobal = roles != null && roles.stream().anyMatch(rol ->
            rol.equalsIgnoreCase("admin") ||
            rol.equalsIgnoreCase("gestor-fe-admin") ||
            rol.equalsIgnoreCase("gestor-fe-cargue")
        );

        if (esAdminOGestorGlobal) {
            // 💼 Administrador / Gestor: Ve el historial completo de cargues de todos los prestadores
            return cargueRepository.findByDeletedAtIsNull(pageable);
        } else {
            // 👤 Prestador: Consulta únicamente los cargues de su usuario (NIT) aplicando la regla
            return cargueRepository.findCarguesVisiblesPrestador(usuario, pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cargue> findByNitPrestador(String desc) {
        return cargueRepository.findByNitPrestador(desc);
    }

    /**
     * 🗑️ Borrado Lógico (Soft Delete) de un Cargue y sus dependencias en cascada
     */
    @Override
    @Transactional
    public Cargue eliminarLogico(Long cargueId) {
        Cargue cargue = cargueRepository.findById(cargueId)
                .orElseThrow(() -> new RuntimeException("No se encontró el registro de cargue con ID: " + cargueId));

        LocalDate fechaActual = LocalDate.now();

        // 1. Marca de borrado lógico en el maestro 'gestor.cargue'
        cargue.setDeletedAt(fechaActual);

        // 2. Si el cargue procesó facturas de forma exitosa, aplicamos el borrado en cascada
        if (Boolean.FALSE.equals(cargue.getExiteError())) {
            List<Factura> facturasAsociadas = facturaRepository.findByIdentificadorCargueAndDeletedAtIsNull(cargueId);

            for (Factura factura : facturasAsociadas) {
                // Marca de borrado lógico en la Factura
                factura.setDeletedAt(fechaActual);

             // Marca de borrado lógico en la Factura
                factura.setDeletedAt(fechaActual);

                // Marca de borrado lógico en todos sus Documentos (XML, PDF y Soportes Congelados)
                if (factura.getDocumentos() != null) {
                    for (Documento doc : factura.getDocumentos()) {
                        doc.setDeletedAt(fechaActual); // Usar fechaActual (LocalDate) en lugar de LocalDateTime.now()
                    }
                }
            }

            // Persistencia atómica de las facturas y sus documentos
            facturaRepository.saveAll(facturasAsociadas);
            LOGGER.info("🗑️ Se realizó el borrado lógico en cascada para {} facturas y sus documentos asociados del cargue #{}",
                    facturasAsociadas.size(), cargueId);
        } else {
            LOGGER.info("🗑️ Borrado lógico ejecutado únicamente sobre el maestro 'cargue' #{} (Cargue con errores).", cargueId);
        }

        return cargueRepository.save(cargue);
    }
}