package com.gestor_fe.admin.service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestor_fe.admin.service.model.entity.TipoIdentificacion;

@Repository
public interface TipoIdentificacionRepository extends JpaRepository<TipoIdentificacion, Long>{

	Page<TipoIdentificacion> findByDeletedAtIsNull(Pageable pageable);
	
}
