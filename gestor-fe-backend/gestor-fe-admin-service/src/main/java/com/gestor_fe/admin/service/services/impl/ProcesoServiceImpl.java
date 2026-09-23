package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.Proceso;
import com.gestor_fe.admin.service.repository.ProcesoRepository;
import com.gestor_fe.admin.service.services.ProcesoService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class ProcesoServiceImpl extends GlobalServiceImpl<Proceso, ProcesoRepository> implements ProcesoService {

    @Override
    public Page<Proceso> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proceso> findByDescripcion(String desc) {
        return repository.findByDescripcion(desc);
    }
}
