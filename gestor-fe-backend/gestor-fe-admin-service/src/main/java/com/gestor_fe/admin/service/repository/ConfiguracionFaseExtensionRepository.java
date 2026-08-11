package com.gestor_fe.admin.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gestor_fe.admin.service.model.entity.ConfiguracionFaseExtension;

@Repository
public interface ConfiguracionFaseExtensionRepository extends JpaRepository<ConfiguracionFaseExtension, Long> {

    Page<ConfiguracionFaseExtension> findByDeletedAtIsNull(Pageable pageable);

    @Query("select x from ConfiguracionFaseExtension x where x.deletedAt is null and (upper(x.codigo) like upper(concat('%', ?1, '%')) or upper(x.descripcion) like upper(concat('%', ?1, '%')))")
    List<ConfiguracionFaseExtension> findByDescripcion(String desc);

    List<ConfiguracionFaseExtension> findByFaseIdAndDeletedAtIsNull(Long faseId);
}