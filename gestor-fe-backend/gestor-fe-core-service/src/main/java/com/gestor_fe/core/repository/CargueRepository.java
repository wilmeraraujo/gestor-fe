package com.gestor_fe.core.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gestor_fe.core.entity.Cargue;

@Repository
public interface CargueRepository extends JpaRepository<Cargue, Long> {
	
	Page<Cargue> findByDeletedAtIsNull(Pageable pageable);

	@Query("select x from Cargue x where deletedAt is null and upper(x.nitPrestador) like upper(concat('%', ?1, '%'))")
	List<Cargue> findByNitPrestador(String desc);
    
}