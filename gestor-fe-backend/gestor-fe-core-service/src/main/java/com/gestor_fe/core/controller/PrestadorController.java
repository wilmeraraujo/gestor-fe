package com.gestor_fe.core.controller;

import java.util.Map;

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
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/prestadores")
public class PrestadorController {

    private final PrestadorService prestadorService;

    public PrestadorController(PrestadorService prestadorService) {
        this.prestadorService = prestadorService;
    }

    // =========================================================================
    // 👤 ENDPOINTS MAESTRO DE PRESTADORES (SUBMÓDULO ADMINISTRACIÓN & MAESTRO)
    // =========================================================================

    @GetMapping("/paginable/buscar")
    public ResponseEntity<Page<Prestador>> buscarPaginado(
            @RequestParam Map<String, String> params,
            Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(prestadorService.buscarPaginado(params, sortedPageable));
    }

    @GetMapping("/paginable/activos")
    public ResponseEntity<Page<Prestador>> listarActivos(Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(prestadorService.findByDeletedAtIsNull(sortedPageable));
    }

    @GetMapping("/paginable")
    public ResponseEntity<Page<Prestador>> listarPaginable(Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(prestadorService.listarPrestadores(sortedPageable));
    }

    @GetMapping("/buscar/{desc}")
    public ResponseEntity<?> buscarPorDescripcion(@PathVariable("desc") String desc) {
        return ResponseEntity.ok(prestadorService.findByDescripcion(desc));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prestador> obtenerPorId(@PathVariable("id") Long id) {
        return prestadorService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/nit/{nit}")
    public ResponseEntity<Prestador> obtenerPorNit(@PathVariable("nit") String nit) {
        return prestadorService.obtenerPorNit(nit)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<Prestador>> listarPrestadores(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "id,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(prestadorService.listarPrestadores(pageable));
    }

    @PostMapping
    public ResponseEntity<Prestador> crear(@RequestBody Prestador prestador) {
        Prestador guardado = prestadorService.crear(prestador);
        return new ResponseEntity<>(guardado, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Prestador> editar(@PathVariable("id") Long id, @RequestBody Prestador prestador) {
        Prestador actualizado = prestadorService.editar(id, prestador);
        return ResponseEntity.ok(actualizado);
    }

    @PutMapping({"/deleted-at/{id}", "/{id}/deleted-at"})
    public ResponseEntity<Prestador> softDelete(@PathVariable("id") Long id) {
        Prestador p = prestadorService.softDelete(id);
        return ResponseEntity.ok(p);
    }

    @RequestMapping(value = {"/toggle-estado/{id}", "/{id}/toggle-estado"}, method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<Prestador> toggleEstado(@PathVariable("id") Long id) {
        Prestador p = prestadorService.toggleEstado(id);
        return ResponseEntity.ok(p);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        prestadorService.eliminar(id);
        return ResponseEntity.noContent().build();
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
            @RequestParam(value = "codigoTipo", required = false) String codigoTipo,
            @RequestParam(value = "tipoId", required = false) String tipoIdFallback,
            @RequestParam(value = "codigoExtension", required = false) String codigoExtension,
            @RequestParam(value = "extensionId", required = false) String extensionIdFallback,
            @RequestPart("archivo") MultipartFile archivo) {

        String tipoFinal = (codigoTipo != null && !codigoTipo.isBlank()) ? codigoTipo : tipoIdFallback;
        String extensionFinal = (codigoExtension != null && !codigoExtension.isBlank()) ? codigoExtension : extensionIdFallback;

        Documento soporteGuardado = prestadorService.cargarSoporte(nitPrestador, tipoFinal, extensionFinal, archivo);
        return new ResponseEntity<>(soporteGuardado, HttpStatus.CREATED);
    }

    /**
     * Consultar soportes de un prestador de forma PAGINADA
     * Ejemplo: GET /api/v1/prestadores/1/soportes?page=0&size=10
     */
    @GetMapping("/{prestadorId}/soportes")
    public ResponseEntity<Page<Documento>> listarSoportesPaginados(
            @PathVariable("prestadorId") Long prestadorId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "id,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Documento> resultado = prestadorService.listarSoportes(prestadorId, pageable);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Realizar Soft Delete de un soporte por su ID de documento
     */
    @DeleteMapping("/soportes/{documentoId}")
    public ResponseEntity<Void> eliminarSoporte(
            @PathVariable("documentoId") Long documentoId) {
        prestadorService.eliminarSoporte(documentoId);
        return ResponseEntity.noContent().build();
    }
}