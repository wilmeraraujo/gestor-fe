package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.Observacion;
import com.gestor_fe.admin.service.repository.ObservacionRepository;
import com.gestor_fe.admin.service.services.ObservacionService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class ObservacionServiceImpl extends GlobalServiceImpl<Observacion ,ObservacionRepository> implements ObservacionService{

	@Override
	public Page<Observacion> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Observacion> findByDescripcion(String desc) {
		return repository.findByDescripcion(desc);
	}
	
	

}
