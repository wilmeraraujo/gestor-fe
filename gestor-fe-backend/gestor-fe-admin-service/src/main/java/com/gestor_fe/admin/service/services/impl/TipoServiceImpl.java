package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.Tipo;
import com.gestor_fe.admin.service.repository.TipoRepository;
import com.gestor_fe.admin.service.services.TipoService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class TipoServiceImpl extends GlobalServiceImpl<Tipo ,TipoRepository> implements TipoService{

	@Override
	public Page<Tipo> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Tipo> findByDescripcion(String desc) {
		return repository.findByDescripcion(desc);
	}
	
}
