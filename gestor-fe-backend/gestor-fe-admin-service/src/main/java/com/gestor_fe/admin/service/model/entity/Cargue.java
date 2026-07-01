package com.gestor_fe.admin.service.model.entity;

import com.service.common.entity.Global;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cargues")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cargue extends Global {

    @Column(nullable = false)
    private String nit;

    @Column(name = "tiene_error", nullable = false)
    private Boolean tieneError;

    @Column(name = "job_execution_id")
    private Long jobExecutionId;

    @Column(name = "nombre_archivo")
    private String nombreArchivo;
}