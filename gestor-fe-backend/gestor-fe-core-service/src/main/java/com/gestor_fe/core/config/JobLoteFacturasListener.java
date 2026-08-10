package com.gestor_fe.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gestor_fe.core.entity.Cargue;
import com.gestor_fe.core.repository.CargueRepository;
import com.gestor_fe.core.service.SseNotificationService;

@Component
public class JobLoteFacturasListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLoteFacturasListener.class);

    @Autowired
    private CargueRepository cargueRepository;

    @Autowired
    private SseNotificationService sseNotificationService; // 👈 Servicio para emitir eventos en tiempo real

    @Override
    public void beforeJob(JobExecution jobExecution) {
        Long identificadorCargue = jobExecution.getJobParameters().getLong("identificadorCargue");
        LOGGER.info("=== 🚀 [beforeJob] Iniciando Job de procesamiento para el cargue ID: {} ===", identificadorCargue);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        JobParameters parameters = jobExecution.getJobParameters();
        Long identificadorCargue = parameters.getLong("identificadorCargue");
        String rutaZipOriginal = parameters.getString("fullPathFileName");
        String usuario = parameters.getString("usuario"); // 👈 Usuario que inició el cargue
        
        // Capturamos el ID asignado por el motor de Spring Batch
        Long jobExecutionId = jobExecution.getId();

        LOGGER.info("=== 🏁 [afterJob] El Job para el cargue ID: {} finalizó con estado: {} ===", 
                identificadorCargue, jobExecution.getStatus());

        // 🗑️ 1. Limpieza preventiva del archivo .zip original
        if (rutaZipOriginal != null) {
            try {
                boolean eliminado = Files.deleteIfExists(Paths.get(rutaZipOriginal));
                if (eliminado) {
                    LOGGER.info("🗑️ Archivo ZIP temporal eliminado con éxito de la ruta de origen.");
                }
            } catch (IOException e) {
                LOGGER.error("❌ Error eliminando el archivo ZIP temporal en: {}", rutaZipOriginal, e);
            }
        }

        // 🔄 2. Actualización del maestro de Control de Cargue en PostgreSQL
        Optional<Cargue> cargueOpt = cargueRepository.findById(identificadorCargue);
        
        if (cargueOpt.isPresent()) {
            Cargue cargue = cargueOpt.get();
            
            cargue.setJobExecutionId(jobExecutionId);

            if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                LOGGER.info("✅ Cargue finalizado de forma exitosa.");
                cargue.setExiteError(false);
                
                // Mapear métricas de lectura total del Step de Spring Batch
                long totalProcesados = jobExecution.getStepExecutions().stream()
                        .mapToLong(se -> se.getWriteCount())
                        .sum();
                cargue.setNumeroRegistro((int) totalProcesados);
                
            } else {
                LOGGER.error("❌ El proceso falló o fue cancelado de forma abrupta.");
                cargue.setExiteError(true);
            }

            Cargue cargueGuardado = cargueRepository.save(cargue);
            LOGGER.info("💾 Registro maestro del cargue actualizado con éxito en la BD. Vinculado a job_execution_id: {}.", jobExecutionId);

            // ⚡ 3. NOTIFICACIÓN SSE EN TIEMPO REAL A ANGULAR
            if (usuario != null && !usuario.isBlank()) {
                sseNotificationService.notificarFinCargue(
                    usuario,
                    cargueGuardado.getId(),
                    Boolean.TRUE.equals(cargueGuardado.getExiteError()),
                    cargueGuardado.getNumeroRegistro()
                );
            }
        } else {
            LOGGER.warn("⚠️ No se encontró ningún registro en 'gestor.cargue' con el ID: {}", identificadorCargue);
        }
    }
}