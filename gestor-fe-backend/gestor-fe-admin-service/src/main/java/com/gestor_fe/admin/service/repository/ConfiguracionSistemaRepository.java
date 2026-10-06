package com.gestor_fe.admin.service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gestor_fe.admin.service.model.entity.ConfiguracionSistema;

@Repository
public interface ConfiguracionSistemaRepository extends JpaRepository<ConfiguracionSistema, Long> {

    Page<ConfiguracionSistema> findByDeletedAtIsNull(Pageable pageable);

    @Query("select x from ConfiguracionSistema x where x.deletedAt is null and (upper(x.codigo) like upper(concat('%', ?1, '%')) or upper(x.descripcion) like upper(concat('%', ?1, '%')))")
    List<ConfiguracionSistema> findByDescripcion(String desc);

    Optional<ConfiguracionSistema> findByCodigoAndDeletedAtIsNull(String codigo);
}