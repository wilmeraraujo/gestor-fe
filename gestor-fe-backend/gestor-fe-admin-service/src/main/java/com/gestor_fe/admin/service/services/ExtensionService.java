package com.gestor_fe.admin.service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.admin.service.model.entity.Extension;
import com.service.common.service.GlobalService;

public interface ExtensionService extends GlobalService<Extension>{

	Page<Extension> findByDeletedAtIsNull(Pageable pageable);
	List<Extension> findByDescripcion(String desc);
	
}
