-- =========================================================================
-- SCRIPT DE CREACIÓN DE TABLAS DE METADATA PARA SPRING BATCH 5 (POSTGRESQL)
-- =========================================================================

CREATE TABLE BATCH_JOB_INSTANCE  (
    JOB_INSTANCE_ID BIGINT  NOT NULL PRIMARY KEY ,
    VERSION BIGINT ,
    JOB_NAME VARCHAR(100) NOT NULL,
    JOB_KEY VARCHAR(32) NOT NULL,
    constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
);

CREATE TABLE BATCH_JOB_EXECUTION  (
    JOB_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
    VERSION BIGINT ,
    JOB_INSTANCE_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP DEFAULT NULL ,
    END_TIME TIMESTAMP DEFAULT NULL ,
    STATUS VARCHAR(10) ,
    EXIT_CODE VARCHAR(2500) ,
    EXIT_MESSAGE VARCHAR(2500) ,
    LAST_UPDATED TIMESTAMP,
    constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
    references BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS  (
    JOB_EXECUTION_ID BIGINT NOT NULL ,
    PARAMETER_NAME VARCHAR(100) NOT NULL ,
    PARAMETER_TYPE VARCHAR(100) NOT NULL ,
    PARAMETER_VALUE VARCHAR(2500) ,
    IDENTIFYING CHAR(1) NOT NULL ,
    constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
    references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION  (
    STEP_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
    VERSION BIGINT NOT NULL,
    STEP_NAME VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP DEFAULT NULL ,
    END_TIME TIMESTAMP DEFAULT NULL ,
    STATUS VARCHAR(10) ,
    COMMIT_COUNT BIGINT ,
    READ_COUNT BIGINT ,
    FILTER_COUNT BIGINT ,
    WRITE_COUNT BIGINT ,
    READ_SKIP_COUNT BIGINT ,
    WRITE_SKIP_COUNT BIGINT ,
    PROCESS_SKIP_COUNT BIGINT ,
    ROLLBACK_COUNT BIGINT ,
    EXIT_CODE VARCHAR(2500) ,
    EXIT_MESSAGE VARCHAR(2500) ,
    LAST_UPDATED TIMESTAMP,
    constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
    references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT  (
    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT ,
    constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
    references BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT  (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT ,
    constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
    references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

-- Secuencias obligatorias para el incremento automático de IDs en Spring Batch 5
CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_INSTANCE_SEQ MAXVALUE 9223372036854775807 NO CYCLE;


--=====================================================================================================
--consultas admin
select * from admin.tipo t;
--consultas
--
select * from gestor.cargue c order by id desc limit 10;
select * from gestor.documento d order by id desc limit 10;
select * from gestor.prestador p order by id desc limit 10;
select * from gestor.factura f order by id desc limit 10;
select * from gestor.error_cargue ec order by id desc limit 10;

select * from public.batch_job_execution bje order by bje.job_execution_id desc limit 10; 
select * from public.batch_job_execution_context bjec; 
select * from public.batch_job_execution_params bjep;--ver 
select * from public.batch_job_instance bji; 
select * from public.batch_step_execution bse;--ver 
select * from public.batch_step_execution_context bsec;

--pasar a fase 1
update gestor.factura 
set estado = 'RADICADO' ,observacion = null , fase_id = 1 , 
causal_devolucion_id = null, numero_causacion=null,tipo_registro_contable=null 
where id in (1);

truncate table gestor.cargue restart identity CASCADE;
truncate table gestor.error_cargue restart identity CASCADE;
truncate table gestor.documento restart identity CASCADE;
truncate table gestor.factura restart identity CASCADE;

truncate table public.batch_job_execution restart identity CASCADE;
truncate table public.batch_job_execution_context restart identity CASCADE; 
truncate table public.batch_job_execution_params restart identity CASCADE; 
truncate table public.batch_job_instance restart identity CASCADE; 
truncate table public.batch_step_execution restart identity CASCADE; 
truncate table public.batch_step_execution_context restart identity CASCADE;

ALTER TABLE gestor.documento ADD COLUMN created_at TIMESTAMP(6) NOT NULL;
ALTER TABLE gestor.factura ADD COLUMN created_at TIMESTAMP(6) NOT NULL;


select * from gestor.cargue car
inner join gestor.factura fac on car.id = fac.identificador_cargue 
inner join gestor.documento doc on fac.id = doc.factura_id;

