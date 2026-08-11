package com.gestor_fe.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.dto.FacturaFilterDto;
import com.gestor_fe.core.dto.GestionDto;
import com.gestor_fe.core.entity.Factura;

import java.util.List;

public interface FacturaService {

    //Consultas de Bandejas por Rol / Fase
    Page<Factura> findByNitAndDeletedAtIsNull(String nit, Pageable pageable);
    
    Page<Factura> findByFaseIdAndDeletedAtIsNull(Long faseId, Pageable pageable);
    
    Page<Factura> findByFaseActiva(Long faseId, Pageable pageable);
    
    Page<Factura> findByDeletedAtIsNull(Pageable pageable);

    //MÉTODO UNIFICADO DE TRANSICIÓN DE FASE (Maneja las 4 Etapas)
    Factura procesarTransicionFase(Long id, Long faseActualId, GestionDto dto);

    //Métodos auxiliares de validación
    List<String> findExistingCufes(List<String> cufes);
    List<String> findExistingNitFacturas(List<String> nitFacturas);
    
    Factura procesarCausacionFase2(Long id, Long tipoRegistroContableId, String numeroCausacion, MultipartFile archivoCausacion);
    
    Factura procesarPagoFase4(Long id, Long tipoRegistroContableId, String numeroCausacion, MultipartFile soporteTb, MultipartFile comprobantePago);

    //BÚSQUEDA DINÁMICA CRITERIA
    Page<Factura> buscarConCriteria(FacturaFilterDto filtro, Pageable pageable);

    Page<Factura> buscarTrazabilidadSegunRol(String nitPrestador, List<String> rolesUsuario, FacturaFilterDto filtro, Pageable pageable);
    
}