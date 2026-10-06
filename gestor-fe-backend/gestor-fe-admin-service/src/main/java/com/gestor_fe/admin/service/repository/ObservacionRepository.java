package com.gestor_fe.admin.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gestor_fe.admin.service.model.entity.Observacion;

public interface ObservacionRepository extends JpaRepository<Observacion, Long>{
	
	Page<Observacion> findByDeletedAtIsNull(Pageable pageable);

	@Query("select x from Observacion x where deletedAt is null and upper(x.descripcion) like upper(concat('%', ?1, '%'))")
	List<Observacion> findByDescripcion(String desc);
	
}
