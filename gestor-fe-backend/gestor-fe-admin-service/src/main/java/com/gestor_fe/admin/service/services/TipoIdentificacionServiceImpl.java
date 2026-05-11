package com.gestor_fe.admin.service.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gestor_fe.admin.service.model.entity.TipoIdentificacion;
import com.gestor_fe.admin.service.repository.TipoIdentificacionRepository;
import com.service.common.service.GlobalServiceImpl;

@Service
public class TipoIdentificacionServiceImpl extends GlobalServiceImpl<TipoIdentificacion ,TipoIdentificacionRepository> implements TipoIdentificacionService{

	@Override
	public Page<TipoIdentificacion> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

}
