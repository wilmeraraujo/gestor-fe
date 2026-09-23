package com.gestor_fe.core.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gestor_fe.core.entity.ErrorCargue;

public interface ErrorCargueRepository extends JpaRepository<ErrorCargue, Long>{

	boolean existsByCargueId(Long cargueId);
	
	List<ErrorCargue> findByCargueId(Long cargueId);	
}
