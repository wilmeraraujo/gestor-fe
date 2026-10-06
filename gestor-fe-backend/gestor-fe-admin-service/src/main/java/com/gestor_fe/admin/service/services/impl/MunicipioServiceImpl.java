package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.Municipio;
import com.gestor_fe.admin.service.repository.MunicipioRepository;
import com.gestor_fe.admin.service.services.MunicipioService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class MunicipioServiceImpl extends GlobalServiceImpl<Municipio, MunicipioRepository> implements MunicipioService {

    @Override
    @Transactional(readOnly = true)
    public Page<Municipio> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Municipio> findByDescripcion(String desc) {
        return repository.findByDescripcion(desc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Municipio> findByDepartamentoId(Long departamentoId) {
        return repository.findByDepartamentoId(departamentoId);
    }
}