package com.gestor_fe.admin.service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestor_fe.admin.service.model.entity.Estado;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Long>{
	
	Page<Estado> findByDeletedAtIsNull(Pageable pageable);

}
