package com.gestor_fe.admin.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gestor_fe.admin.service.model.entity.Municipio;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    Page<Municipio> findByDeletedAtIsNull(Pageable pageable);

    @Query("select m from Municipio m where m.deletedAt is null and upper(m.descripcion) like upper(concat('%', ?1, '%'))")
    List<Municipio> findByDescripcion(String desc);

    // Búsqueda específica de municipios por el ID de su departamento
    @Query("select m from Municipio m where m.deletedAt is null and m.departamento.id = ?1")
    List<Municipio> findByDepartamentoId(Long departamentoId);
}