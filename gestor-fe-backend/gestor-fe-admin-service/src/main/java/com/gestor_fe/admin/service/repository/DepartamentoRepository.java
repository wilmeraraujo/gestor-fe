package com.gestor_fe.admin.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gestor_fe.admin.service.model.entity.Departamento;

@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {

    Page<Departamento> findByDeletedAtIsNull(Pageable pageable);

    @Query("select d from Departamento d where d.deletedAt is null and upper(d.descripcion) like upper(concat('%', ?1, '%'))")
    List<Departamento> findByDescripcion(String desc);
}