package com.gestor_fe.admin.service.services.impl;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gestor_fe.admin.service.model.entity.UnidadMedida;
import com.gestor_fe.admin.service.repository.UnidadMedidaRepository;
import com.gestor_fe.admin.service.services.UnidadMedidaService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class UnidadMedidaServiceImpl extends GlobalServiceImpl<UnidadMedida, UnidadMedidaRepository> implements UnidadMedidaService {

    @Override
    public Page<UnidadMedida> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnidadMedida> findByDescripcion(String desc) {
        return repository.findByDescripcion(desc);
    }
}
