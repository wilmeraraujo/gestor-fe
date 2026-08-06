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

--insertar prestador
insert into gestor.prestador (created_at,direccion,email,identificador_cargue,nit,razon_social,telefono)
values ('2026-07-24 15:10:19.336','Calle 15 # 24-50','facturacion@clinicasoluciones.com',0,123,'CLINICA SOLUCIONES SALUD S.A.S','3001234567');

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

select * from departamento d order by id desc;

INSERT INTO admin.departamento (codigo, descripcion, created_at) VALUES
('05', 'ANTIOQUIA', NOW()),
('08', 'ATLÁNTICO', NOW()),
('11', 'BOGOTÁ, D.C.', NOW()),
('13', 'BOLÍVAR', NOW()),
('15', 'BOYACÁ', NOW()),
('17', 'CALDAS', NOW()),
('18', 'CAQUETÁ', NOW()),
('19', 'CAUCA', NOW()),
('20', 'CESAR', NOW()),
('23', 'CÓRDOBA', NOW()),
('25', 'CUNDINAMARCA', NOW()),
('27', 'CHOCO', NOW()),
('41', 'HUILA', NOW()),
('44', 'LA GUAJIRA', NOW()),
('47', 'MAGDALENA', NOW()),
('50', 'META', NOW()),
('52', 'NARIÑO', NOW()),
('54', 'NORTE DE SANTANDER', NOW()),
('63', 'QUINDÍO', NOW()),
('66', 'RISARALDA', NOW()),
('68', 'SANTANDER', NOW()),
('70', 'SUCRE', NOW()),
('73', 'TOLIMA', NOW()),
('76', 'VALLE DEL CAUCA', NOW()),
('81', 'ARAUCA', NOW()),
('85', 'CASANARE', NOW()),
('86', 'PUTUMAYO', NOW()),
('88', 'SAN ANDRÉS Y PROVIDENCIA', NOW()),
('91', 'AMAZONAS', NOW()),
('94', 'GUAINÍA', NOW()),
('97', 'VAUPÉS', NOW()),
('99', 'VICHADA', NOW());



INSERT INTO admin.municipio (codigo, descripcion, departamento_id, created_at) VALUES
-- Antioquia
('05001', 'MEDELLÍN', (SELECT id FROM admin.departamento WHERE codigo = '05'), NOW()),
('05088', 'BELLO', (SELECT id FROM admin.departamento WHERE codigo = '05'), NOW()),
('05266', 'ENVIGADO', (SELECT id FROM admin.departamento WHERE codigo = '05'), NOW()),
('05360', 'ITAGÜÍ', (SELECT id FROM admin.departamento WHERE codigo = '05'), NOW()),
('05615', 'RIONEGRO', (SELECT id FROM admin.departamento WHERE codigo = '05'), NOW()),

-- Atlántico
('08001', 'BARRANQUILLA', (SELECT id FROM admin.departamento WHERE codigo = '08'), NOW()),
('08758', 'SOLEDAD', (SELECT id FROM admin.departamento WHERE codigo = '08'), NOW()),
('08520', 'PUERTO COLOMBIA', (SELECT id FROM admin.departamento WHERE codigo = '08'), NOW()),

-- Bogotá D.C.
('11001', 'BOGOTÁ, D.C.', (SELECT id FROM admin.departamento WHERE codigo = '11'), NOW()),

-- Bolívar
('13001', 'CARTAGENA DE INDIAS', (SELECT id FROM admin.departamento WHERE codigo = '13'), NOW()),
('13430', 'MAGANGUÉ', (SELECT id FROM admin.departamento WHERE codigo = '13'), NOW()),

-- Boyacá
('15001', 'TUNJA', (SELECT id FROM admin.departamento WHERE codigo = '15'), NOW()),
('15238', 'DUITAMA', (SELECT id FROM admin.departamento WHERE codigo = '15'), NOW()),
('15759', 'SOGAMOSO', (SELECT id FROM admin.departamento WHERE codigo = '15'), NOW()),

-- Caldas
('17001', 'MANIZALES', (SELECT id FROM admin.departamento WHERE codigo = '17'), NOW()),
('17380', 'LA DORADA', (SELECT id FROM admin.departamento WHERE codigo = '17'), NOW()),

-- Caquetá
('18001', 'FLORENCIA', (SELECT id FROM admin.departamento WHERE codigo = '18'), NOW()),

-- Cauca
('19001', 'POPAYÁN', (SELECT id FROM admin.departamento WHERE codigo = '19'), NOW()),
('19698', 'SANTANDER DE QUILICHAO', (SELECT id FROM admin.departamento WHERE codigo = '19'), NOW()),

-- Cesar
('20001', 'VALLEDUPAR', (SELECT id FROM admin.departamento WHERE codigo = '20'), NOW()),
('20011', 'AGUACHICA', (SELECT id FROM admin.departamento WHERE codigo = '20'), NOW()),

-- Córdoba
('23001', 'MONTERÍA', (SELECT id FROM admin.departamento WHERE codigo = '23'), NOW()),

-- Cundinamarca
('25175', 'CHÍA', (SELECT id FROM admin.departamento WHERE codigo = '25'), NOW()),
('25269', 'FACATATIVÁ', (SELECT id FROM admin.departamento WHERE codigo = '25'), NOW()),
('25290', 'FUSAGASUGÁ', (SELECT id FROM admin.departamento WHERE codigo = '25'), NOW()),
('25754', 'SOACHA', (SELECT id FROM admin.departamento WHERE codigo = '25'), NOW()),
('25899', 'ZIPAQUIRÁ', (SELECT id FROM admin.departamento WHERE codigo = '25'), NOW()),

-- Chocó
('27001', 'QUIBDÓ', (SELECT id FROM admin.departamento WHERE codigo = '27'), NOW()),

-- Huila
('41001', 'NEIVA', (SELECT id FROM admin.departamento WHERE codigo = '41'), NOW()),
('41551', 'PITALITO', (SELECT id FROM admin.departamento WHERE codigo = '41'), NOW()),

-- La Guajira
('44001', 'RIOHACHA', (SELECT id FROM admin.departamento WHERE codigo = '44'), NOW()),
('44430', 'MAICAO', (SELECT id FROM admin.departamento WHERE codigo = '44'), NOW()),

-- Magdalena
('47001', 'SANTA MARTA', (SELECT id FROM admin.departamento WHERE codigo = '47'), NOW()),

-- Meta
('50001', 'VILLAVICENCIO', (SELECT id FROM admin.departamento WHERE codigo = '50'), NOW()),

-- Nariño
('52001', 'PASTO', (SELECT id FROM admin.departamento WHERE codigo = '52'), NOW()),
('52356', 'IPIALES', (SELECT id FROM admin.departamento WHERE codigo = '52'), NOW()),
('52838', 'TÚQUERRES', (SELECT id FROM admin.departamento WHERE codigo = '52'), NOW()),
('52835', 'TUMACO', (SELECT id FROM admin.departamento WHERE codigo = '52'), NOW()),

-- Norte de Santander
('54001', 'CÚCUTA', (SELECT id FROM admin.departamento WHERE codigo = '54'), NOW()),
('54498', 'OCAÑA', (SELECT id FROM admin.departamento WHERE codigo = '54'), NOW()),
('54518', 'PAMPLONA', (SELECT id FROM admin.departamento WHERE codigo = '54'), NOW()),

-- Quindío
('63001', 'ARMENIA', (SELECT id FROM admin.departamento WHERE codigo = '63'), NOW()),

-- Risaralda
('66001', 'PEREIRA', (SELECT id FROM admin.departamento WHERE codigo = '66'), NOW()),
('66170', 'DOSQUEBRADAS', (SELECT id FROM admin.departamento WHERE codigo = '66'), NOW()),

-- Santander
('68001', 'BUCARAMANGA', (SELECT id FROM admin.departamento WHERE codigo = '68'), NOW()),
('68081', 'BARRANCABERMEJA', (SELECT id FROM admin.departamento WHERE codigo = '68'), NOW()),
('68276', 'FLORIDABLANCA', (SELECT id FROM admin.departamento WHERE codigo = '68'), NOW()),
('68307', 'GIRÓN', (SELECT id FROM admin.departamento WHERE codigo = '68'), NOW()),
('68547', 'PIEDECUESTA', (SELECT id FROM admin.departamento WHERE codigo = '68'), NOW()),

-- Sucre
('70001', 'SINCELEJO', (SELECT id FROM admin.departamento WHERE codigo = '70'), NOW()),

-- Tolima
('73001', 'IBAGUÉ', (SELECT id FROM admin.departamento WHERE codigo = '73'), NOW()),
('73268', 'ESPINAL', (SELECT id FROM admin.departamento WHERE codigo = '73'), NOW()),

-- Valle del Cauca
('76001', 'CALI', (SELECT id FROM admin.departamento WHERE codigo = '76'), NOW()),
('76109', 'BUENAVENTURA', (SELECT id FROM admin.departamento WHERE codigo = '76'), NOW()),
('76111', 'GUADALAJARA DE BUGA', (SELECT id FROM admin.departamento WHERE codigo = '76'), NOW()),
('76520', 'PALMIRA', (SELECT id FROM admin.departamento WHERE codigo = '76'), NOW()),
('76834', 'TULUÁ', (SELECT id FROM admin.departamento WHERE codigo = '76'), NOW()),

-- Arauca
('81001', 'ARAUCA', (SELECT id FROM admin.departamento WHERE codigo = '81'), NOW()),

-- Casanare
('85001', 'YOPAL', (SELECT id FROM admin.departamento WHERE codigo = '85'), NOW()),

-- Putumayo
('86001', 'MOCOA', (SELECT id FROM admin.departamento WHERE codigo = '86'), NOW()),
('86568', 'PUERTO ASÍS', (SELECT id FROM admin.departamento WHERE codigo = '86'), NOW()),

-- San Andrés y Providencia
('88001', 'SAN ANDRÉS', (SELECT id FROM admin.departamento WHERE codigo = '88'), NOW()),

-- Amazonas
('91001', 'LETICIA', (SELECT id FROM admin.departamento WHERE codigo = '91'), NOW()),

-- Guainía
('94001', 'INÍRIDA', (SELECT id FROM admin.departamento WHERE codigo = '94'), NOW()),

-- Vaupés
('97001', 'MITÚ', (SELECT id FROM admin.departamento WHERE codigo = '97'), NOW()),

-- Vichada
('99001', 'PUERTO CARREÑO', (SELECT id FROM admin.departamento WHERE codigo = '99'), NOW());