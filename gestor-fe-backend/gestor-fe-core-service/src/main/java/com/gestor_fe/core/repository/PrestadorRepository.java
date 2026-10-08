package com.gestor_fe.core.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.gestor_fe.core.entity.Prestador;

@Repository
public interface PrestadorRepository extends JpaRepository<Prestador, Long>, JpaSpecificationExecutor<Prestador> {
    Optional<Prestador> findByNitAndDeletedAtIsNull(String nit);
    Optional<Prestador> findByNit(String nit);
    Page<Prestador> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT p FROM Prestador p WHERE " +
           "(LOWER(p.nit) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(p.razonSocial) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(p.telefono) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(p.direccion) LIKE LOWER(CONCAT('%', :filtro, '%')))")
    List<Prestador> buscarPorTexto(@Param("filtro") String filtro);
}