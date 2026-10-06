package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.Movimiento;
import com.gestor_fe.admin.service.repository.MovimientoRepository;
import com.gestor_fe.admin.service.services.MovimientoService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class MovimientoServiceImpl extends GlobalServiceImpl<Movimiento, MovimientoRepository> implements MovimientoService {

	@Override
	public Page<Movimiento> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Movimiento> findByDescripcion(String desc) {
		return repository.findByDescripcion(desc);
	}

}
