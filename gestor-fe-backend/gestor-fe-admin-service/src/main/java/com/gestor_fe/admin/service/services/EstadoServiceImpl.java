package com.gestor_fe.admin.service.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gestor_fe.admin.service.model.entity.Estado;
import com.gestor_fe.admin.service.repository.EstadoRepository;
import com.service.common.service.GlobalServiceImpl;

@Service
public class EstadoServiceImpl extends GlobalServiceImpl<Estado ,EstadoRepository> implements EstadoService{

	@Override
	public Page<Estado> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

}
