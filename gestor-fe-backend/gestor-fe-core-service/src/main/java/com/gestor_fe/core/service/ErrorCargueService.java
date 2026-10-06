package com.gestor_fe.core.service;

import java.io.IOException;
import java.util.List;

import com.gestor_fe.core.entity.ErrorCargue;

public interface ErrorCargueService {

	public Iterable<ErrorCargue> saveAll(Iterable<ErrorCargue> errores);

	public boolean validarCargueError(Long identificadorCargue);

	public List<ErrorCargue> obtenerErrorCargue(Long identificadorCargue);

	public byte[] exportErrorCargueToExcel(Long identificadorCargue) throws IOException;
	
}
