package com.service.common.service;

import java.util.List;

import com.service.common.entity.LogAccion;

public interface LogAccionService {

	LogAccion registrarLog(String modulo, Long registroId, String accion, String observacion, String usuarioUsername);

	List<LogAccion> obtenerHistorial(String modulo, Long registroId);

}
