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
@Table(name = "configuracion_sistema", schema = "admin")
public class ConfiguracionSistema extends Global {

    @Column(name = "valor", nullable = false, length = 255)
    private String valor;

    @Column(name = "categoria", length = 50)
    private String categoria;
}