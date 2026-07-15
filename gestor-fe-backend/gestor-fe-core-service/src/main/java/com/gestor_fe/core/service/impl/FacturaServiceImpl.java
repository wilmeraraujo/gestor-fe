package com.gestor_fe.core.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.repository.FacturaRepository;
import com.gestor_fe.core.service.FacturaService;

@Service
public class FacturaServiceImpl implements FacturaService{

	private final FacturaRepository repository;
	
	public FacturaServiceImpl(FacturaRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public Page<Factura> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}

}
