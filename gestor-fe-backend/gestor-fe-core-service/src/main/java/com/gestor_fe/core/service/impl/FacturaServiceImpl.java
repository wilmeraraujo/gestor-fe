package com.gestor_fe.core.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.repository.FacturaRepository;
import com.gestor_fe.core.service.FacturaService;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacturaServiceImpl implements FacturaService{

	private final FacturaRepository repository;
	
	public FacturaServiceImpl(FacturaRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public Page<Factura> findByDeletedAtIsNull(Pageable pageable) {
		return repository.findByDeletedAtIsNull(pageable);
	}
	
	@Override
    @Transactional(readOnly = true)
    public List<String> findExistingCufes(List<String> cufes) {
        if (cufes == null || cufes.isEmpty()) {
            return List.of();
        }
        return repository.findExistingCufes(cufes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findExistingNitFacturas(List<String> nitFacturas) {
        if (nitFacturas == null || nitFacturas.isEmpty()) {
            return List.of();
        }
        return repository.findExistingNitFacturas(nitFacturas);
    }

}
