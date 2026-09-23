package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.CausalDevolucion;
import com.service.common.service.GlobalService;

public interface CausalDevolucionService extends GlobalService<CausalDevolucion>{

	Page<CausalDevolucion> findByDeletedAtIsNull(Pageable pageable);
	List<CausalDevolucion> findByDescripcion(String desc);
	
}
