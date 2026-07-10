package com.gestor_fe.core.service.impl;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.core.entity.Cargue;
import com.gestor_fe.core.repository.CargueRepository;
import com.gestor_fe.core.service.CargueService;

@Service
public class CargueServiceImpl implements CargueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CargueServiceImpl.class);

    @Autowired
    private CargueRepository cargueRepository;

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
    @Async("taskExecutor") // <--- Conexión con tu AsyncConfig
    public void runBatchJobAsynchronously(File fileToImport, Cargue cargue) {
        try {
            LOGGER.info("=== 🚀 Hilo secundario arrancando Job de Spring Batch de forma asíncrona ===");
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("fullPathFileName", fileToImport.getAbsolutePath())
                    .addLong("identificadorCargue", cargue.getId())
                    .addString("nombreArchivo", cargue.getNombreArchivo())
                    .addString("usuario", cargue.getUsuario())
                    .addLong("timestamp", System.currentTimeMillis()) // Evita duplicidad de parámetros en ejecuciones
                    .toJobParameters();

            jobLauncher.run(procesarLoteFacturasJob, jobParameters);
            
        } catch (Exception e) {
            LOGGER.error("❌ Error crítico ejecutando el Job Asíncrono de Facturas: ", e);
        }
    }
}