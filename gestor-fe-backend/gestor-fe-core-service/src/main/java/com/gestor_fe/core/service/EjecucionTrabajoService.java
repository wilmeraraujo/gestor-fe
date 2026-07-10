package com.gestor_fe.core.service;

import java.util.Optional;

import com.gestor_fe.core.entity.EjecucionTrabajo;

public interface EjecucionTrabajoService {

	public Optional<EjecucionTrabajo> finById(Long id);
	
}
