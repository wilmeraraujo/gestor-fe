package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.Tipo;
import com.service.common.service.GlobalService;

public interface TipoService extends GlobalService<Tipo>{

	Page<Tipo> findByDeletedAtIsNull(Pageable pageable);
	List<Tipo> findByDescripcion(String desc);
	
}
