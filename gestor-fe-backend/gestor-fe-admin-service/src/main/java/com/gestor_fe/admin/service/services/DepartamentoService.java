package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.Departamento;
import com.service.common.service.GlobalService;

public interface DepartamentoService extends GlobalService<Departamento> {

    Page<Departamento> findByDeletedAtIsNull(Pageable pageable);
    List<Departamento> findByDescripcion(String desc);
}