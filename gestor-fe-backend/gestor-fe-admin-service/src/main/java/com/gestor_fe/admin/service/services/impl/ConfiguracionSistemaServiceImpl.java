package com.gestor_fe.admin.service.services.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.ConfiguracionSistema;
import com.gestor_fe.admin.service.repository.ConfiguracionSistemaRepository;
import com.gestor_fe.admin.service.services.ConfiguracionSistemaService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class ConfiguracionSistemaServiceImpl extends GlobalServiceImpl<ConfiguracionSistema, ConfiguracionSistemaRepository> implements ConfiguracionSistemaService {

    @Override
    @Transactional(readOnly = true)
    public Page<ConfiguracionSistema> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionSistema> findByDescripcion(String desc) {
        return repository.findByDescripcion(desc);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionSistema> findByCodigo(String codigo) {
        return repository.findByCodigoAndDeletedAtIsNull(codigo);
    }
}