package com.gestor_fe.admin.service.services;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.gestor_fe.admin.service.model.entity.ResponsabilidadFiscal;
import com.service.common.service.GlobalService;

public interface ResponsabilidadFiscalService extends GlobalService<ResponsabilidadFiscal> {
    Page<ResponsabilidadFiscal> findByDeletedAtIsNull(Pageable pageable);
    List<ResponsabilidadFiscal> findByDescripcion(String desc);
}
