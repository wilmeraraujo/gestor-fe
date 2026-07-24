package com.gestor_fe.core.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Prestador;
import com.gestor_fe.core.service.PrestadorService;

@RestController
@RequestMapping("/api/v1/prestadores")
public class PrestadorController {

    private final PrestadorService prestadorService;

    public PrestadorController(PrestadorService prestadorService) {
        this.prestadorService = prestadorService;
    }

    // =========================================================================
    // 👤 ENDPOINTS MAESTRO DE PRESTADORES
    // =========================================================================

    @PostMapping
    public ResponseEntity<Prestador> crearOActualizar(@RequestBody Prestador prestador) {
        Prestador guardado = prestadorService.crearOActualizarPrestador(prestador);
        return new ResponseEntity<>(guardado, HttpStatus.CREATED);
    }

    @GetMapping("/nit/{nit}")
    public ResponseEntity<Prestador> obtenerPorNit(@PathVariable String nit) {
        return prestadorService.obtenerPorNit(nit)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<Prestador>> listarPrestadores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(prestadorService.listarPrestadores(pageable));
    }

    // =========================================================================
    // 📎 ENDPOINTS PARA GESTIÓN DE SOPORTES (RUT, CAMARA DE COMERCIO, ETC.)
    // =========================================================================

    /**
     * Cargar un soporte asignado al Prestador
     * Ejemplo Multipart Form-Data: nitPrestador=900123456, tipoId=3, extensionId=2, archivo=[File]
     */
    @PostMapping(value = "/soportes/cargar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Documento> cargarSoporte(
            @RequestParam("nitPrestador") String nitPrestador,
            @RequestParam("tipoId") Long tipoId,
            @RequestParam("extensionId") Long extensionId,
            @RequestPart("archivo") MultipartFile archivo) {

        Documento soporteGuardado = prestadorService.cargarSoporte(nitPrestador, tipoId, extensionId, archivo);
        return new ResponseEntity<>(soporteGuardado, HttpStatus.CREATED);
    }

    /**
     * Consultar soportes de un prestador de forma PAGINADA
     * Ejemplo: GET /api/v1/prestadores/1/soportes?page=0&size=10
     */
    @GetMapping("/{prestadorId}/soportes")
    public ResponseEntity<Page<Documento>> listarSoportesPaginados(
            @PathVariable Long prestadorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Documento> resultado = prestadorService.listarSoportes(prestadorId, pageable);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Realizar Soft Delete de un soporte por su ID de documento
     */
    @DeleteMapping("/soportes/{documentoId}")
    public ResponseEntity<Void> eliminarSoporte(@PathVariable Long documentoId) {
        prestadorService.eliminarSoporte(documentoId);
        return ResponseEntity.noContent().build();
    }
}