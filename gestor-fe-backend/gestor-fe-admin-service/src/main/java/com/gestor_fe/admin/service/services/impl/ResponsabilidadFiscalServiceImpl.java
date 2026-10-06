package com.gestor_fe.admin.service.services.impl;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gestor_fe.admin.service.model.entity.ResponsabilidadFiscal;
import com.gestor_fe.admin.service.repository.ResponsabilidadFiscalRepository;
import com.gestor_fe.admin.service.services.ResponsabilidadFiscalService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class ResponsabilidadFiscalServiceImpl extends GlobalServiceImpl<ResponsabilidadFiscal, ResponsabilidadFiscalRepository> implements ResponsabilidadFiscalService {

    @Override
    public Page<ResponsabilidadFiscal> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponsabilidadFiscal> findByDescripcion(String desc) {
        return repository.findByDescripcion(desc);
    }
}
