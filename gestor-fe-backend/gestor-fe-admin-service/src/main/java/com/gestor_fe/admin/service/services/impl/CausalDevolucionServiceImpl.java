package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.CausalDevolucion;
import com.gestor_fe.admin.service.repository.CausalDevolucionRepsitory;
import com.gestor_fe.admin.service.services.CausalDevolucionService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class CausalDevolucionServiceImpl extends GlobalServiceImpl<CausalDevolucion ,CausalDevolucionRepsitory> implements CausalDevolucionService{

	@Override
	public Page<CausalDevolucion> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CausalDevolucion> findByDescripcion(String desc) {
		return repository.findByDescripcion(desc);
	}
	
}
