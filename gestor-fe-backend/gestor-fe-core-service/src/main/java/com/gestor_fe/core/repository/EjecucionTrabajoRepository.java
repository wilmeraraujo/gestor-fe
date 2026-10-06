package com.gestor_fe.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestor_fe.core.entity.EjecucionTrabajo;

@Repository
public interface EjecucionTrabajoRepository extends JpaRepository<EjecucionTrabajo, Long>{

}
