package com.gestor_fe.admin.service.services;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.gestor_fe.admin.service.model.entity.MedioPago;
import com.service.common.service.GlobalService;

public interface MedioPagoService extends GlobalService<MedioPago> {
    Page<MedioPago> findByDeletedAtIsNull(Pageable pageable);
    List<MedioPago> findByDescripcion(String desc);
}
