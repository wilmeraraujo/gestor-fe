package com.gestor_fe.core.service.impl;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gestor_fe.core.entity.ErrorCargue;
import com.gestor_fe.core.repository.ErrorCargueRepository;
import com.gestor_fe.core.service.ErrorCargueService;
import com.gestor_fe.core.service.ExportarExcelService;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ErrorCargueServiceImpl implements ErrorCargueService{

	private final ErrorCargueRepository repository;
	private final ExportarExcelService<ErrorCargue> exportarExcelService;
	
	public ErrorCargueServiceImpl(ErrorCargueRepository repository
			,ExportarExcelService<ErrorCargue> exportarExcelService) {
		this.repository = repository;
		this.exportarExcelService = exportarExcelService;
	}
	
	@Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
	public Iterable<ErrorCargue> saveAll(Iterable<ErrorCargue> errores) {
		return repository.saveAll(errores);
	}

	@Override
	public boolean validarCargueError(Long identificadorCargue) {
		return repository.existsByCargueId(identificadorCargue);
	}

	@Override
	public List<ErrorCargue> obtenerErrorCargue(Long identificadorCargue) {
		return repository.findByCargueId(identificadorCargue);
	}

	@Override
	public byte[] exportErrorCargueToExcel(Long identificadorCargue) throws IOException {
		List<ErrorCargue> errores = repository.findByCargueId(identificadorCargue);
		return exportarExcelService.exportToExcel(errores);
	}

}
