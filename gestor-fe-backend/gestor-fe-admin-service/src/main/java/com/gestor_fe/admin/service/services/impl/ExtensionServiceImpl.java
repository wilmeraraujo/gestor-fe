package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.Extension;
import com.gestor_fe.admin.service.repository.ExtensionRepository;
import com.gestor_fe.admin.service.services.ExtensionService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class ExtensionServiceImpl extends GlobalServiceImpl<Extension ,ExtensionRepository> implements ExtensionService{

	@Override
	public Page<Extension> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Extension> findByDescripcion(String desc) {
		return repository.findByDescripcion(desc);
	}
	
}
