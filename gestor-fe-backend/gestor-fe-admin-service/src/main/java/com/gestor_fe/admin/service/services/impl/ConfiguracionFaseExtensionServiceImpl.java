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
    
    @Override
    @Transactional
    public ConfiguracionFaseExtension save(ConfiguracionFaseExtension entity) {
        boolean existe;
        
        if (entity.getId() == null) {
            // Creación: Verificar si la combinación fase_id + extension_id ya existe
            existe = repository.existsByFaseIdAndExtensionIdAndDeletedAtIsNull(
                entity.getFaseId(), entity.getExtensionId()
            );
        } else {
            // Edición: Verificar que la combinación no pertenezca a OTRO registro distinto
            existe = repository.existsByFaseIdAndExtensionIdAndIdNotAndDeletedAtIsNull(
                entity.getFaseId(), entity.getExtensionId(), entity.getId()
            );
        }

        if (existe) {
            throw new IllegalArgumentException("Ya existe una regla configurada para esta extensión en la fase seleccionada.");
        }

        return super.save(entity);
    }
}