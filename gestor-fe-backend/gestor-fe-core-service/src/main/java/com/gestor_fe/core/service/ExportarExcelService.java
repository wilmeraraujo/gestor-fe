package com.gestor_fe.core.service;

import java.io.IOException;
import java.util.List;

public interface ExportarExcelService<E> {

	byte[] exportToExcel(List<E> data) throws IOException;
	
}
