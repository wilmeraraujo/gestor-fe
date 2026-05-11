package com.gestor_fe.admin.service.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.TipoIdentificacion;
import com.service.common.service.GlobalService;

public interface TipoIdentificacionService extends GlobalService<TipoIdentificacion>{

	public Page<TipoIdentificacion> findByDeletedAtIsNull(Pageable pageable);
	
}
