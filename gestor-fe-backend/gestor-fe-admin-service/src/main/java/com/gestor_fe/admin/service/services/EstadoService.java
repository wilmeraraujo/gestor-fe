package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.Estado;
import com.service.common.service.GlobalService;

public interface EstadoService extends GlobalService<Estado>{

	Page<Estado> findByDeletedAtIsNull(Pageable pageable);
	List<Estado> findByDescripcion(String desc);
}
