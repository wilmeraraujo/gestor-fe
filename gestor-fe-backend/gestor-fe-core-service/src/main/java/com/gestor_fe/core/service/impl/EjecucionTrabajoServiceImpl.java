package com.gestor_fe.core.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gestor_fe.core.entity.EjecucionTrabajo;
import com.gestor_fe.core.repository.EjecucionTrabajoRepository;
import com.gestor_fe.core.service.EjecucionTrabajoService;

@Service
public class EjecucionTrabajoServiceImpl implements EjecucionTrabajoService{
	 
	private final EjecucionTrabajoRepository repository;
	
	public EjecucionTrabajoServiceImpl (EjecucionTrabajoRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public Optional<EjecucionTrabajo> finById(Long id) {
		return repository.findById(id);
	}

}
