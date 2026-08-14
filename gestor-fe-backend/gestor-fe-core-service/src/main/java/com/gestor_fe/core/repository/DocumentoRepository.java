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

import com.gestor_fe.core.entity.Documento;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long>, JpaSpecificationExecutor<Documento> {
    
    // 📋 1. Obtener todos los documentos activos paginados (General)
    Page<Documento> findByDeletedAtIsNull(Pageable pageable);

    // 📄 2. Obtener los soportes de un prestador específico de forma PAGINADA
    Page<Documento> findByPrestadorIdAndDeletedAtIsNull(Long prestadorId, Pageable pageable);

    // 🧾 3. Obtener los documentos de una factura de forma PAGINADA
    Page<Documento> findByFacturaIdAndDeletedAtIsNull(Long facturaId, Pageable pageable);

    // 🔍 4. Buscar un documento específico por tipo (Útil para saber si ya existe el RUT o Cámara de Comercio)
    Optional<Documento> findByPrestadorIdAndTipoIdAndDeletedAtIsNull(Long prestadorId, Long tipoId);
    
    
    /**
     * Busca todos los soportes empresariales activos asignados a un prestador por su NIT
     * (los soportes del prestador tienen factura_id IS NULL)
     */
    @Query("SELECT d FROM Documento d WHERE d.deletedAt IS NULL AND d.factura IS NULL AND d.prestador.nit = :nit")
    List<Documento> findSoportesPrestadorByNit(@Param("nit") String nit);
}