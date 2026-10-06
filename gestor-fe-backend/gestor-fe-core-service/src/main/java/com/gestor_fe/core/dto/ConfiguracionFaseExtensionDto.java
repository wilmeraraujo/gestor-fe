package com.gestor_fe.core.dto;

import lombok.Data;

@Data
public class ConfiguracionFaseExtensionDto {
    private Long id;
    private Long faseId;
    private Long extensionId;
    private String codigo;
    private String descripcion;
    private Integer tamanoMaximoMb;
    private Boolean obligatorio;
    private Boolean permiteMultiple;
}