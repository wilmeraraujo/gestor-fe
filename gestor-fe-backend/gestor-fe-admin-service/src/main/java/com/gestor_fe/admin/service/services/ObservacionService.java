package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.Observacion;
import com.service.common.service.GlobalService;

public interface ObservacionService extends GlobalService<Observacion>{

	Page<Observacion> findByDeletedAtIsNull(Pageable pageable);
	List<Observacion> findByDescripcion(String desc);
}
