package com.gestor_fe.core.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TipoDto {
    private Long id;
    private String codigo;
    private String descripcion;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}