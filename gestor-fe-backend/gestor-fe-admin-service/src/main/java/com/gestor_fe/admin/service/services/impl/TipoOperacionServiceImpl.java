package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gestor_fe.admin.service.model.entity.TipoOperacion;
import com.gestor_fe.admin.service.repository.TipoOperacionRepository;
import com.gestor_fe.admin.service.services.TipoOperacionService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class TipoOperacionServiceImpl extends GlobalServiceImpl<TipoOperacion, TipoOperacionRepository> implements TipoOperacionService {

    @Override
    public Page<TipoOperacion> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    @Override
    public List<TipoOperacion> findByDescripcion(String desc) {
        return repository.findByDescripcion(desc);
    }
}
