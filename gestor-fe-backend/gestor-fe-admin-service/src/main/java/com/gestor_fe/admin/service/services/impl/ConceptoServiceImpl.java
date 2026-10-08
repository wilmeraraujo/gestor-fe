package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.Concepto;
import com.gestor_fe.admin.service.repository.ConceptoRepository;
import com.gestor_fe.admin.service.services.ConceptoService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class ConceptoServiceImpl extends GlobalServiceImpl<Concepto, ConceptoRepository> implements ConceptoService {

	@Override
	public Page<Concepto> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Concepto> findByDeletedAtIsNull() {
		return repository.findByDeletedAtIsNull();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Concepto> findByDescripcion(String desc) {
		return repository.findByDescripcion(desc);
	}
	
}
