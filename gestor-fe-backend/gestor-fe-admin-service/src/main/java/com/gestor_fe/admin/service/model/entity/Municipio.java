package com.gestor_fe.admin.service.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.service.common.entity.Global;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "municipio", schema = "admin")
public class Municipio extends Global {

    // Relación Muchos Municipios -> 1 Departamento
    @JsonIgnoreProperties(value = {"municipios", "hibernateLazyInitializer", "handler"}, allowSetters = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;
}