package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.ConfiguracionFaseExtension;
import com.gestor_fe.admin.service.repository.ConfiguracionFaseExtensionRepository;
import com.gestor_fe.admin.service.services.ConfiguracionFaseExtensionService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class ConfiguracionFaseExtensionServiceImpl extends GlobalServiceImpl<ConfiguracionFaseExtension, ConfiguracionFaseExtensionRepository> implements ConfiguracionFaseExtensionService {

    @Override
    @Transactional(readOnly = true)
    public Page<ConfiguracionFaseExtension> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionFaseExtension> findByDescripcion(String desc) {
        return repository.findByDescripcion(desc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionFaseExtension> findByFaseId(Long faseId) {
        return repository.findByFaseIdAndDeletedAtIsNull(faseId);
    }
}