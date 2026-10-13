package com.gestor_fe.admin.service.services;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.Proceso;
import com.service.common.service.GlobalService;

public interface ProcesoService extends GlobalService<Proceso> {
    Page<Proceso> findByDeletedAtIsNull(Pageable pageable);
    List<Proceso> findByDescripcion(String desc);
}
