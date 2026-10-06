package com.gestor_fe.admin.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gestor_fe.admin.service.model.entity.Movimiento;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

	Page<Movimiento> findByDeletedAtIsNull(Pageable pageable);

	@Query("select x from Movimiento x where deletedAt is null and upper(x.descripcion) like upper(concat('%', ?1, '%'))")
	List<Movimiento> findByDescripcion(String desc);

}
