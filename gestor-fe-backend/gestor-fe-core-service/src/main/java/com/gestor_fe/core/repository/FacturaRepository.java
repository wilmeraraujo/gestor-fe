package com.gestor_fe.core.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gestor_fe.core.entity.Factura;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    // 👤 Prestador: Ve todas sus facturas (sin importar estado o fase)
    Page<Factura> findByNitAndDeletedAtIsNull(String nit, Pageable pageable);

    // 👷 Rol F1 (Gestión): Ve facturas en Fase 1 con estado "RADICADO", "EN GESTIÓN" o "FACTURA NO CONFORME"
    Page<Factura> findByFaseIdAndDeletedAtIsNull(Long faseId, Pageable pageable);

    // 💼 Rol F2, F3, F4: Ve facturas de una fase específica que NO estén anuladas/rechazadas
    @Query("SELECT f FROM Factura f WHERE f.faseId = :faseId AND f.deletedAt IS NULL AND f.estado NOT IN ('FACTURA NO CONFORME', 'DEVOLVER FACTURA ELECTRÓNICA', 'DEVOLUCIÓN')")
    Page<Factura> findByFaseActiva(@Param("faseId") Long faseId, Pageable pageable);

    // 🔍 Rol F5 (Seguimiento): Ve TODAS las facturas del sistema (Trazabilidad global)
    Page<Factura> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT f.cufe FROM Factura f WHERE f.deletedAt IS NULL AND f.cufe IN :cufes")
    List<String> findExistingCufes(@Param("cufes") List<String> cufes);

    @Query("SELECT CONCAT(f.nit, '_', f.numeroFactura) FROM Factura f WHERE f.deletedAt IS NULL AND CONCAT(f.nit, '_', f.numeroFactura) IN :nitFacturas")
    List<String> findExistingNitFacturas(@Param("nitFacturas") List<String> nitFacturas);
}