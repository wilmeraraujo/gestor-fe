package com.gestor_fe.admin.service.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.ConfiguracionSistema;
import com.service.common.service.GlobalService;

public interface ConfiguracionSistemaService extends GlobalService<ConfiguracionSistema> {

    Page<ConfiguracionSistema> findByDeletedAtIsNull(Pageable pageable);
    List<ConfiguracionSistema> findByDescripcion(String desc);
    Optional<ConfiguracionSistema> findByCodigo(String codigo);
}