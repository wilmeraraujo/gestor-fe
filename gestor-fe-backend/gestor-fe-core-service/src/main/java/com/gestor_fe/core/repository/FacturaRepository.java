package com.gestor_fe.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestor_fe.core.entity.Factura;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Integer> {
	
	Page<Factura> findByDeletedAtIsNull(Pageable pageable);
    
}