package com.gestor_fe.admin.service.model.entity;

import com.service.common.entity.Global;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "errores_cargue")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationError extends Global {

    @Column(name = "job_execution_id")
    private Long jobExecutionId;

    private String archivo;
    private String error;
}