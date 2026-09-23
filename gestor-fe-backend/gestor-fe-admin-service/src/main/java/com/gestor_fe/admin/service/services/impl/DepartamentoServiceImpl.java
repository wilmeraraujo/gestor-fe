package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.Departamento;
import com.gestor_fe.admin.service.repository.DepartamentoRepository;
import com.gestor_fe.admin.service.services.DepartamentoService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class DepartamentoServiceImpl extends GlobalServiceImpl<Departamento, DepartamentoRepository> implements DepartamentoService {

    @Override
    @Transactional(readOnly = true)
    public Page<Departamento> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Departamento> findByDescripcion(String desc) {
        return repository.findByDescripcion(desc);
    }
}