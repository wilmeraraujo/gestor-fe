package com.gestor_fe.admin.service.services.impl;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gestor_fe.admin.service.model.entity.MedioPago;
import com.gestor_fe.admin.service.repository.MedioPagoRepository;
import com.gestor_fe.admin.service.services.MedioPagoService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class MedioPagoServiceImpl extends GlobalServiceImpl<MedioPago, MedioPagoRepository> implements MedioPagoService {

    @Override
    public Page<MedioPago> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedioPago> findByDescripcion(String desc) {
        return repository.findByDescripcion(desc);
    }
}
