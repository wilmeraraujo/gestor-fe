package com.gestor_fe.admin.service.model.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.service.common.entity.Global;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "departamento", schema = "admin")
public class Departamento extends Global {

    // Relación 1 Departamento -> Muchos Municipios
    @JsonIgnoreProperties(value = {"departamento", "hibernateLazyInitializer", "handler"}, allowSetters = true)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "departamento", cascade = CascadeType.ALL)
    private List<Municipio> municipios = new ArrayList<>();
}