package com.gestor_fe.admin.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gestor_fe.admin.service.model.entity.Estado;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Long>{
	
	Page<Estado> findByDeletedAtIsNull(Pageable pageable);

	@Query("select x from Estado x where upper(x.descripcion) like upper(concat('%', ?1, '%'))")
	List<Estado> findByDescripcion(String desc);
	
}
