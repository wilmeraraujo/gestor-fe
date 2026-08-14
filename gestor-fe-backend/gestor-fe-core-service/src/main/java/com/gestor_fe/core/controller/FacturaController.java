package com.gestor_fe.core.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.dto.FacturaFilterDto;
import com.gestor_fe.core.dto.GestionDto;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.service.FacturaService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/factura")
public class FacturaController {

    private final FacturaService service;

    public FacturaController(FacturaService service) {
        this.service = service;
    }

    // =========================================================================
    // 📋 CONSULTAS Y BANDEJAS DE LECTURA
    // =========================================================================

    @GetMapping("/prestador/{nit}")
    public ResponseEntity<?> findByNit(@PathVariable("nit") String nit, Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(service.findByNitAndDeletedAtIsNull(nit, sorted));
    }

    @GetMapping("/fase/1")
    public ResponseEntity<?> listarFase1(Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(service.findByFaseIdAndDeletedAtIsNull(1L, sorted));
    }

    @GetMapping("/fase/{faseId}")
    public ResponseEntity<?> listarFaseActiva(@PathVariable("faseId") Long faseId, Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(service.findByFaseActiva(faseId, sorted));
    }

    @GetMapping("/seguimiento")
    public ResponseEntity<?> listarSeguimiento(Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(service.findByDeletedAtIsNull(sorted));
    }

    // =========================================================================
    // 🔍 BÚSQUEDA DINÁMICA JPA CRITERIA Y TRAZABILIDAD
    // =========================================================================

    @PostMapping("/buscar-criteria")
    public ResponseEntity<?> buscarConCriteria(@RequestBody FacturaFilterDto filtro, Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(service.buscarConCriteria(filtro, sorted));
    }

    @PostMapping("/trazabilidad/buscar")
    public ResponseEntity<?> buscarTrazabilidadSegunRol(
            @RequestParam(value = "nitPrestador", required = false) String nitPrestador,
            @RequestParam(value = "roles", required = false) List<String> roles,
            @RequestBody(required = false) FacturaFilterDto filtro,
            Pageable pageable) {

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(service.buscarTrazabilidadSegunRol(nitPrestador, roles, filtro, sorted));
    }

    // =========================================================================
    // ⚙️ ENDPOINTS DE TRANSICIÓN Y GESTIÓN DE ETAPAS (CON USUARIO)
    // =========================================================================

    // ⚙️ Transición Estándar por JSON (Fases 1, 3, 4 y rechazos)
    @PutMapping("/{id}/procesar-fase/{faseId}")
    public ResponseEntity<Factura> procesarTransicion(
            @PathVariable("id") Long id,
            @PathVariable("faseId") Long faseId,
            @RequestBody GestionDto dto) {

        return ResponseEntity.ok(service.procesarTransicionFase(id, faseId, dto));
    }

    // 🏦 FASE 2: Procesar causación con PDF adjunto + Usuario
    @PostMapping(value = "/{id}/causacion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Factura> procesarCausacionFase2(
            @PathVariable("id") Long id,
            @RequestParam("tipoRegistroContableId") Long tipoRegistroContableId,
            @RequestParam("numeroCausacion") String numeroCausacion,
            @RequestParam(value = "usuario", required = false) String usuario, // 👈 Captura el usuario
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {

        return ResponseEntity.ok(service.procesarCausacionFase2(id, tipoRegistroContableId, numeroCausacion, usuario, archivo));
    }

    // 💸 FASE 4: Registrar pago y soportes + Usuario
    @PostMapping(value = "/{id}/pago", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Factura> procesarPagoFase4(
            @PathVariable("id") Long id,
            @RequestParam(value = "tipoRegistroContableId", required = false) Long tipoRegistroContableId,
            @RequestParam(value = "numeroCausacion", required = false) String numeroCausacion,
            @RequestParam(value = "usuario", required = false) String usuario, // 👈 Captura el usuario
            @RequestParam(value = "soporteTb", required = false) MultipartFile soporteTb,
            @RequestParam(value = "comprobantePago", required = false) MultipartFile comprobantePago) {

        return ResponseEntity.ok(service.procesarPagoFase4(id, tipoRegistroContableId, numeroCausacion, usuario, soporteTb, comprobantePago));
    }
}