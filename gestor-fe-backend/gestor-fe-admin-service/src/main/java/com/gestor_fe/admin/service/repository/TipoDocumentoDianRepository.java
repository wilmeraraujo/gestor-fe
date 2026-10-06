package com.gestor_fe.admin.service.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.gestor_fe.admin.service.model.entity.TipoDocumentoDian;

@Repository
public interface TipoDocumentoDianRepository extends JpaRepository<TipoDocumentoDian, Long> {

    Page<TipoDocumentoDian> findByDeletedAtIsNull(Pageable pageable);

    @Query("select x from TipoDocumentoDian x where deletedAt is null and upper(x.descripcion) like upper(concat('%', ?1, '%'))")
    List<TipoDocumentoDian> findByDescripcion(String desc);

    @Query("select x from TipoDocumentoDian x where deletedAt is null and x.codigo = ?1")
    TipoDocumentoDian findByCodigo(String codigo);
}
