package com.gestor_fe.core.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gestor_fe.core.entity.Factura;

public interface FacturaService {
	
	Page<Factura> findByDeletedAtIsNull(Pageable pageable);	
	List<String> findExistingCufes(List<String> cufes);
    List<String> findExistingNitFacturas(List<String> nitFacturas);

}
