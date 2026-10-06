package com.gestor_fe.admin.service.services;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.gestor_fe.admin.service.model.entity.UnidadMedida;
import com.service.common.service.GlobalService;

public interface UnidadMedidaService extends GlobalService<UnidadMedida> {
    Page<UnidadMedida> findByDeletedAtIsNull(Pageable pageable);
    List<UnidadMedida> findByDescripcion(String desc);
}
