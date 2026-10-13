package com.gestor_fe.admin.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gestor_fe.admin.service.model.entity.TipoOperacion;

@Repository
public interface TipoOperacionRepository extends JpaRepository<TipoOperacion, Long> {

    Page<TipoOperacion> findByDeletedAtIsNull(Pageable pageable);

    @Query("select x from TipoOperacion x where deletedAt is null and upper(x.descripcion) like upper(concat('%', ?1, '%'))")
    List<TipoOperacion> findByDescripcion(String desc);

    @Query("select x from TipoOperacion x where deletedAt is null and x.codigo = ?1")
    TipoOperacion findByCodigo(String codigo);
}
