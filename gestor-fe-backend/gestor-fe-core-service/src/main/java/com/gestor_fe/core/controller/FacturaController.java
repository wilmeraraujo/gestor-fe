package com.gestor_fe.core.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    // 📋 CONSULTAS Y BANDEJAS DE LECTURA (PAGINADAS Y ORDENADAS POR ID DESC)
    // =========================================================================

    // 📋 Bandeja Prestador
    @GetMapping("/prestador/{nit}")
    public ResponseEntity<?> findByNit(@PathVariable("nit") String nit, Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(service.findByNitAndDeletedAtIsNull(nit, sorted));
    }

    // 📋 Bandeja Fase 1
    @GetMapping("/fase/1")
    public ResponseEntity<?> listarFase1(Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(service.findByFaseIdAndDeletedAtIsNull(1L, sorted));
    }

    // 📋 Bandeja Fases Activas (Fase 2, 3, 4)
    @GetMapping("/fase/{faseId}")
    public ResponseEntity<?> listarFaseActiva(@PathVariable("faseId") Long faseId, Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(service.findByFaseActiva(faseId, sorted));
    }

    // 📋 Módulo de Seguimiento (Fase 5 / Global)
    @GetMapping("/seguimiento")
    public ResponseEntity<?> listarSeguimiento(Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(service.findByDeletedAtIsNull(sorted));
    }

    // =========================================================================
    // ⚙️ ENDPOINTS DE TRANSICIÓN Y GESTIÓN DE ETAPAS
    // =========================================================================

    // ⚙️ Transición estándar por JSON/DTO (Fases 1, 2, 3 y 4 sin adjuntos o rechazos)
    @PutMapping("/{id}/procesar-fase/{faseId}")
    public ResponseEntity<Factura> procesarTransicion(
            @PathVariable("id") Long id,
            @PathVariable("faseId") Long faseId,
            @RequestBody GestionDto dto) {

        return ResponseEntity.ok(service.procesarTransicionFase(id, faseId, dto));
    }

    // 🏦 ENDPOINT FASE 2: Procesar causación con archivo PDF adjunto
    @PostMapping(value = "/{id}/causacion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Factura> procesarCausacionFase2(
            @PathVariable("id") Long id,
            @RequestParam("tipoRegistroContableId") Long tipoRegistroContableId, // 👈 Actualizado a Long
            @RequestParam("numeroCausacion") String numeroCausacion,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {

        return ResponseEntity.ok(service.procesarCausacionFase2(id, tipoRegistroContableId, numeroCausacion, archivo));
    }

    // 💸 ENDPOINT FASE 4: Registrar pago y cargar soportes (Documento TB + Comprobante)
    @PostMapping(value = "/{id}/pago", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Factura> procesarPagoFase4(
            @PathVariable("id") Long id,
            @RequestParam(value = "tipoRegistroContableId", required = false) Long tipoRegistroContableId, // 👈 Actualizado a Long
            @RequestParam(value = "numeroCausacion", required = false) String numeroCausacion,
            @RequestParam(value = "soporteTb", required = false) MultipartFile soporteTb,
            @RequestParam(value = "comprobantePago", required = false) MultipartFile comprobantePago) {

        return ResponseEntity.ok(service.procesarPagoFase4(id, tipoRegistroContableId, numeroCausacion, soporteTb, comprobantePago));
    }
}