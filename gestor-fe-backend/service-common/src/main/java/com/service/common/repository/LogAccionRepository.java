package com.service.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.service.common.entity.LogAccion;

@Repository
public interface LogAccionRepository extends JpaRepository<LogAccion, Long> {

	List<LogAccion> findByModuloAndRegistroIdOrderByCreatedAtDesc(String modulo, Long registroId);

	List<LogAccion> findByModuloOrderByCreatedAtDesc(String modulo);

}
