package com.gestor_fe.core.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.dto.FacturaFilterDto;
import com.gestor_fe.core.dto.GestionDto;
import com.gestor_fe.core.entity.Factura;

public interface FacturaService {

    Page<Factura> findByNitAndDeletedAtIsNull(String nit, Pageable pageable);
    Page<Factura> findByFaseIdAndDeletedAtIsNull(Long faseId, Pageable pageable);
    Page<Factura> findByFaseActiva(Long faseId, Pageable pageable);
    Page<Factura> findByDeletedAtIsNull(Pageable pageable);

    // TRANSICIÓN DE FASE CON DTO
    Factura procesarTransicionFase(Long id, Long faseActualId, GestionDto dto);

    // MÉTODOS MULTIPART CON PARÁMETRO 'USUARIO'
    Factura procesarCausacionFase2(Long id, Long tipoRegistroContableId, String numeroCausacion, String usuario, MultipartFile archivoCausacion);
    Factura procesarPagoFase4(Long id, Long tipoRegistroContableId, String numeroCausacion, String usuario, MultipartFile soporteTb, MultipartFile comprobantePago);

    // AUXILIARES Y CRITERIA
    List<String> findExistingCufes(List<String> cufes);
    List<String> findExistingNitFacturas(List<String> nitFacturas);
    Page<Factura> buscarConCriteria(FacturaFilterDto filtro, Pageable pageable);
    Page<Factura> buscarTrazabilidadSegunRol(String nitPrestador, List<String> rolesUsuario, FacturaFilterDto filtro, Pageable pageable);
}