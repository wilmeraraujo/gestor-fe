package com.gestor_fe.admin.service.model.entity;

import com.service.common.entity.Global;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "configuracion_fase_extension", schema = "admin")
public class ConfiguracionFaseExtension extends Global {

    @Column(name = "fase_id", nullable = false)
    private Long faseId;

    @Column(name = "extension_id", nullable = false)
    private Long extensionId;

    @Column(name = "tamano_maximo_mb", nullable = false)
    private Integer tamanoMaximoMb = 10;

    @Column(name = "obligatorio", nullable = false)
    private Boolean obligatorio = false;

    @Column(name = "permite_multiple", nullable = false)
    private Boolean permiteMultiple = true;
}