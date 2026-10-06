package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.Clasificacion;
import com.service.common.service.GlobalService;

public interface ClasificacionService extends GlobalService<Clasificacion>{

	Page<Clasificacion> findByDeletedAtIsNull(Pageable pageable);
	List<Clasificacion> findByDescripcion(String desc);
	
}
