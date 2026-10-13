package com.gestor_fe.admin.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.gestor_fe.admin.service.model.entity.Concepto;

@Repository
public interface ConceptoRepository extends JpaRepository<Concepto, Long> {

	Page<Concepto> findByDeletedAtIsNull(Pageable pageable);

	List<Concepto> findByDeletedAtIsNull();

	@Query("select x from Concepto x where deletedAt is null and upper(x.descripcion) like upper(concat('%', ?1, '%'))")
	List<Concepto> findByDescripcion(String desc);
	
}
