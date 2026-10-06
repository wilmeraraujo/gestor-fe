package com.gestor_fe.admin.service.services;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.gestor_fe.admin.service.model.entity.TipoDocumentoDian;
import com.service.common.service.GlobalService;

public interface TipoDocumentoDianService extends GlobalService<TipoDocumentoDian> {
    Page<TipoDocumentoDian> findByDeletedAtIsNull(Pageable pageable);
    List<TipoDocumentoDian> findByDescripcion(String desc);
}
