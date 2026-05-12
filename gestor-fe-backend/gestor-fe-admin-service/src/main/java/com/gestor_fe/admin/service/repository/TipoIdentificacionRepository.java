package com.gestor_fe.admin.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gestor_fe.admin.service.model.entity.TipoIdentificacion;

@Repository
public interface TipoIdentificacionRepository extends JpaRepository<TipoIdentificacion, Long>{

	Page<TipoIdentificacion> findByDeletedAtIsNull(Pageable pageable);
	
	@Query("select x from TipoIdentificacion x where upper(x.descripcion) like upper(concat('%', ?1, '%'))")
	List<TipoIdentificacion> findByDescripcion(String desc);
	
}
