package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.Concepto;
import com.service.common.service.GlobalService;

public interface ConceptoService extends GlobalService<Concepto> {

	Page<Concepto> findByDeletedAtIsNull(Pageable pageable);
	List<Concepto> findByDeletedAtIsNull();
	List<Concepto> findByDescripcion(String desc);
	
}
