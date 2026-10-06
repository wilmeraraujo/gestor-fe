package com.gestor_fe.admin.service.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.admin.service.model.entity.TipoIdentificacion;
import com.gestor_fe.admin.service.repository.TipoIdentificacionRepository;
import com.gestor_fe.admin.service.services.TipoIdentificacionService;
import com.service.common.service.GlobalServiceImpl;

@Service
public class TipoIdentificacionServiceImpl extends GlobalServiceImpl<TipoIdentificacion ,TipoIdentificacionRepository> implements TipoIdentificacionService{

	@Override
	public Page<TipoIdentificacion> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TipoIdentificacion> findByDescripcion(String desc) {
		return repository.findByDescripcion(desc);
	}

}
