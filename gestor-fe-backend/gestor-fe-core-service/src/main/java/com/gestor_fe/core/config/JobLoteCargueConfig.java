package com.gestor_fe.core.config;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.gestor_fe.core.client.AdminFeignClient;
import com.gestor_fe.core.dto.FacturaZipWrapperDto;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.repository.DocumentoRepository;
import com.gestor_fe.core.repository.ErrorCargueRepository;
import com.gestor_fe.core.repository.FacturaRepository;
import com.gestor_fe.core.service.ErrorCargueService;
import com.gestor_fe.core.service.FacturaService;
import com.gestor_fe.core.step.FacturaZipItemReader;
import com.gestor_fe.core.step.FacturaZipProcessor;
import com.gestor_fe.core.step.FacturaZipWriter;

@Configuration
public class JobLoteCargueConfig {  
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final FacturaRepository facturaRepository;
    private final DocumentoRepository documentoRepository;
    private final ErrorCargueService errorCargueService;
    private final FacturaService facturaService;

    @Value("${ruta.storage}")
    private String rutaStorage;
    
    @Value("${ruta.storage.validos}")
    private String rutaStorageValidos;

    public JobLoteCargueConfig(JobRepository jobRepository, 
                               PlatformTransactionManager transactionManager,
                               FacturaService facturaService,
                               FacturaRepository facturaRepository,
                               DocumentoRepository documentoRepository,
                               ErrorCargueService errorCargueService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.facturaService = facturaService;
        this.facturaRepository = facturaRepository;
        this.documentoRepository = documentoRepository;
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
          .skipLimit(Integer.MAX_VALUE)
          .build();
    }

    @Bean
    @StepScope
    public ItemReader<FacturaZipWrapperDto> itemReader(
            @Value("#{jobParameters['fullPathFileName']}") String fullPathFileName,
            @Value("#{jobParameters['identificadorCargue']}") Long identificadorCargue,
            ErrorCargueRepository errorCargueRepository,
            PlatformTransactionManager transactionManager) { // 👈 Inyección de Spring
        return new FacturaZipItemReader(fullPathFileName, errorCargueRepository, transactionManager, identificadorCargue);
    }

    @Bean
    @StepScope
    public ItemProcessor<FacturaZipWrapperDto, Factura> itemProcessor(
            @Value("#{jobParameters['identificadorCargue']}") Long identificadorCargue,
            FacturaService facturaService,
            ErrorCargueService errorCargueService,
            DocumentoRepository documentoRepository,
            AdminFeignClient adminFeignClient) { // 👈 Inyección declarativa de Spring
        return new FacturaZipProcessor(identificadorCargue, facturaService, errorCargueService, documentoRepository, adminFeignClient);
    }

    @Bean
    @StepScope
    public ItemWriter<Factura> writer(
            @Value("#{jobParameters['identificadorCargue']}") Long identificadorCargue) {
        return new FacturaZipWriter(facturaRepository, documentoRepository, rutaStorageValidos, identificadorCargue);
    }
}