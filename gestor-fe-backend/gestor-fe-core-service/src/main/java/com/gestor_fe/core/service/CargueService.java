package com.gestor_fe.core.service;

import java.io.File;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.gestor_fe.core.entity.Cargue;

public interface CargueService {
    Cargue save(Cargue cargue);
    void runBatchJobAsynchronously(File fileToImport, Cargue cargue);
    Page<Cargue> findByDeletedAtIsNull(Pageable pageable);
	List<Cargue> findByNitPrestador(String desc);
}