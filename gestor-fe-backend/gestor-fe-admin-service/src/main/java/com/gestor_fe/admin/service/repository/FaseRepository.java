package com.gestor_fe.admin.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.gestor_fe.admin.service.model.entity.Fase;

@Repository
public interface FaseRepository extends JpaRepository<Fase, Long>{

	Page<Fase> findByDeletedAtIsNull(Pageable pageable);

	@Query("select x from Fase x where deletedAt is null and upper(x.descripcion) like upper(concat('%', ?1, '%'))")
	List<Fase> findByDescripcion(String desc);
	
}
