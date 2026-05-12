package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.TipoIdentificacion;
import com.service.common.service.GlobalService;

public interface TipoIdentificacionService extends GlobalService<TipoIdentificacion>{

	Page<TipoIdentificacion> findByDeletedAtIsNull(Pageable pageable);
	List<TipoIdentificacion> findByDescripcion(String desc);
	
}
