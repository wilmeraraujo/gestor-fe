package com.gestor_fe.core.config;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.gestor_fe.core.dto.FacturaZipWrapperDto;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.repository.FacturaRepository;
import com.gestor_fe.core.service.ErrorCargueService;
import com.gestor_fe.core.step.FacturaZipItemReader;
import com.gestor_fe.core.step.FacturaZipProcessor;
import com.gestor_fe.core.step.FacturaZipWriter;

@Configuration
public class JobLoteCargueConfig {  
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final FacturaRepository facturaRepository;
    private final ErrorCargueService errorCargueService;

    @Value("${ruta.storage}")
    private String rutaStorage;
    
    @Value("${ruta.storage.validos}")
    private String rutaStorageValidos;

    // Inyección por constructor limpia y consistente
    public JobLoteCargueConfig(JobRepository jobRepository, 
                               PlatformTransactionManager transactionManager,
                               FacturaRepository facturaRepository,
                               ErrorCargueService errorCargueService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.facturaRepository = facturaRepository;
        this.errorCargueService = errorCargueService;
    }

    @Bean
    public Job procesarLoteFacturasJob(JobLoteFacturasListener listener, Step stepOne) throws Exception {
        return new JobBuilder("procesarLoteFacturasJob", jobRepository) 
                .listener(listener)
                .start(stepOne)
                .build();
    }

    @Bean
    public Step stepOne(
        ItemReader<FacturaZipWrapperDto> reader,
        ItemProcessor<FacturaZipWrapperDto, Factura> processor,
        ItemWriter<Factura> writer
    ) throws Exception {
      return new StepBuilder("stepOne", jobRepository) 
          .<FacturaZipWrapperDto, Factura>chunk(20, transactionManager)
          .reader(reader)
          .processor(processor)
          .writer(writer)
          .faultTolerant()
          .noSkip(IllegalStateException.class)
          .skip(Exception.class)
          .skipLimit(Integer.MAX_VALUE)
          .build();
    }

    @Bean
    @StepScope
    public ItemReader<FacturaZipWrapperDto> reader(
            @Value("#{jobParameters['fullPathFileName']}") String zipFilePath,
            @Value("#{jobParameters['identificadorCargue']}") Long identificadorCargue) { 
        // CORREGIDO: Le pasamos el Servicio de errores en lugar del Repositorio
        return new FacturaZipItemReader(zipFilePath, errorCargueService, identificadorCargue); 
    }

    @Bean
    @StepScope
    public ItemProcessor<FacturaZipWrapperDto, Factura> processor(
            @Value("#{jobParameters['identificadorCargue']}") Long identificadorCargue) {
        return new FacturaZipProcessor(identificadorCargue);
    }

    @Bean
    @StepScope
    public ItemWriter<Factura> writer(
            @Value("#{jobParameters['identificadorCargue']}") Long identificadorCargue) {
        return new FacturaZipWriter(facturaRepository, rutaStorageValidos, identificadorCargue);
    }
}