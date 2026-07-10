package com.gestor_fe.core.service;

import java.io.File;
import com.gestor_fe.core.entity.Cargue;

public interface CargueService {
    Cargue save(Cargue cargue);
    void runBatchJobAsynchronously(File fileToImport, Cargue cargue);
}