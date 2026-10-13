package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.Movimiento;
import com.service.common.service.GlobalService;

public interface MovimientoService extends GlobalService<Movimiento> {

	Page<Movimiento> findByDeletedAtIsNull(Pageable pageable);
	List<Movimiento> findByDescripcion(String desc);

}
