package com.gestor_fe.admin.service.services.impl;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gestor_fe.admin.service.model.entity.TipoDocumentoDian;
import com.gestor_fe.admin.service.repository.TipoDocumentoDianRepository;
import com.gestor_fe.admin.service.services.TipoDocumentoDianService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class TipoDocumentoDianServiceImpl extends GlobalServiceImpl<TipoDocumentoDian, TipoDocumentoDianRepository> implements TipoDocumentoDianService {

    @Override
    public Page<TipoDocumentoDian> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoDocumentoDian> findByDescripcion(String desc) {
        return repository.findByDescripcion(desc);
    }
}
