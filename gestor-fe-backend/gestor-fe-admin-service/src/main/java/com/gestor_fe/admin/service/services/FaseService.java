package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.Fase;
import com.service.common.service.GlobalService;

public interface FaseService extends GlobalService<Fase>{

	Page<Fase> findByDeletedAtIsNull(Pageable pageable);
	List<Fase> findByDescripcion(String desc);
	
}
