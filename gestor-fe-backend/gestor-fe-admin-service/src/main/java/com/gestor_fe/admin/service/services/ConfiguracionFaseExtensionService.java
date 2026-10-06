package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.ConfiguracionFaseExtension;
import com.service.common.service.GlobalService;

public interface ConfiguracionFaseExtensionService extends GlobalService<ConfiguracionFaseExtension> {

    Page<ConfiguracionFaseExtension> findByDeletedAtIsNull(Pageable pageable);
    List<ConfiguracionFaseExtension> findByDescripcion(String desc);
    List<ConfiguracionFaseExtension> findByFaseId(Long faseId);
    ConfiguracionFaseExtension save(ConfiguracionFaseExtension entity);
}