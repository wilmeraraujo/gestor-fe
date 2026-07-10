package com.gestor_fe.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gestor_fe.core.entity.Cargue;

@Repository
public interface CargueRepository extends JpaRepository<Cargue, Long> {
    
}