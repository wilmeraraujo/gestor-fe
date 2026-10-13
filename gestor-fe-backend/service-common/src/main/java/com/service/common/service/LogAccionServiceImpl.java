package com.service.common.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.common.entity.LogAccion;
import com.service.common.repository.LogAccionRepository;

@Service
public class LogAccionServiceImpl implements LogAccionService {

	@Autowired
	private LogAccionRepository logAccionRepository;

	@Override
	@Transactional
	public LogAccion registrarLog(String modulo, Long registroId, String accion, String observacion, String usuarioUsername) {
		String finalUsername = (usuarioUsername != null && !usuarioUsername.trim().isEmpty()) 
				? usuarioUsername.trim() 
				: "SISTEMA";

		LogAccion log = LogAccion.builder()
				.modulo(modulo != null ? modulo.toUpperCase() : "GENERAL")
				.registroId(registroId)
				.accion(accion != null ? accion.toUpperCase() : "ACCION")
				.observacion(observacion)
				.usuarioUsername(finalUsername)
				.createdAt(LocalDateTime.now())
				.build();
		return logAccionRepository.save(log);
	}

	@Override
	@Transactional(readOnly = true)
	public List<LogAccion> obtenerHistorial(String modulo, Long registroId) {
		if (modulo != null && registroId != null) {
			return logAccionRepository.findByModuloAndRegistroIdOrderByCreatedAtDesc(modulo.toUpperCase(), registroId);
		}
		if (modulo != null) {
			return logAccionRepository.findByModuloOrderByCreatedAtDesc(modulo.toUpperCase());
		}
		return logAccionRepository.findAll();
	}

}
