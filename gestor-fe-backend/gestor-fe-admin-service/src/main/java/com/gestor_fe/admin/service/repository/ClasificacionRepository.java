package com.gestor_fe.admin.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gestor_fe.admin.service.model.entity.Clasificacion;

@Repository
public interface ClasificacionRepository extends JpaRepository<Clasificacion, Long> {

	Page<Clasificacion> findByDeletedAtIsNull(Pageable pageable);

	@Query("select x from Clasificacion x where deletedAt is null and upper(x.descripcion) like upper(concat('%', ?1, '%'))")
	List<Clasificacion> findByDescripcion(String desc);
	
}
