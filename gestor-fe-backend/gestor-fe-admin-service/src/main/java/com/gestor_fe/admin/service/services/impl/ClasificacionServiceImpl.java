package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.Clasificacion;
import com.gestor_fe.admin.service.repository.ClasificacionRepository;
import com.gestor_fe.admin.service.services.ClasificacionService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class ClasificacionServiceImpl extends GlobalServiceImpl<Clasificacion ,ClasificacionRepository> implements ClasificacionService{

	@Override
	public Page<Clasificacion> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Clasificacion> findByDescripcion(String desc) {
		return repository.findByDescripcion(desc);
	}
}
