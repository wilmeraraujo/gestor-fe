package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.Municipio;
import com.service.common.service.GlobalService;

public interface MunicipioService extends GlobalService<Municipio> {

    Page<Municipio> findByDeletedAtIsNull(Pageable pageable);
    List<Municipio> findByDescripcion(String desc);
    List<Municipio> findByDepartamentoId(Long departamentoId);
}