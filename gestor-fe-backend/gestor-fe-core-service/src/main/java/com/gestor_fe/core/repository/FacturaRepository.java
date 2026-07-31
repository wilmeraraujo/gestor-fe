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
	
	// 🔍 Obtener facturas activas filtradas por NIT del prestador
    Page<Factura> findByNitAndDeletedAtIsNull(String nit, Pageable pageable);
	
	
	Page<Factura> findByDeletedAtIsNull(Pageable pageable);
	
	// 1. Devuelve la lista de CUFEs que ya existen en la base de datos
    @Query("SELECT f.cufe FROM Factura f WHERE f.deletedAt IS NULL AND f.cufe IN :cufes")
    List<String> findExistingCufes(@Param("cufes") List<String> cufes);

    // 2. Devuelve la lista de combinaciones "NIT_NUMEROFACTURA" que ya existen en la BD
    @Query("SELECT CONCAT(f.nit, '_', f.numeroFactura) FROM Factura f WHERE f.deletedAt IS NULL AND CONCAT(f.nit, '_', f.numeroFactura) IN :nitFacturas")
    List<String> findExistingNitFacturas(@Param("nitFacturas") List<String> nitFacturas);
    
}