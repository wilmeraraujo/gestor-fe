package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.Estado;
import com.gestor_fe.admin.service.repository.EstadoRepository;
import com.service.common.service.GlobalServiceImpl;

@Service
public class EstadoServiceImpl extends GlobalServiceImpl<Estado ,EstadoRepository> implements EstadoService{

	@Override
	public Page<Estado> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Estado> findByDescripcion(String desc) {
		return repository.findByDescripcion(desc);
	}
	
	

}
