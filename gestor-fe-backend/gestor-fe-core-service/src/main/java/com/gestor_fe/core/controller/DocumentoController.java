package com.gestor_fe.core.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;

import com.gestor_fe.core.service.DocumentoService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/documento")
public class DocumentoController {
    
    private final DocumentoService service;
    
    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @GetMapping("/paginable/activos")
    public ResponseEntity<?> listAll(Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok().body(service.findByDeletedAtIsNull(sortedPageable));
    }

    // 🔍 1. BUSCADOR CON FILTRADO DINÁMICO COMBINADO
    @GetMapping("/paginable/buscar")
    public ResponseEntity<?> buscarDocumentos(
            @RequestParam(value = "numeroFactura", required = false) String numeroFactura, 
            @RequestParam(value = "nit", required = false) String nit,                     
            @RequestParam(value = "tipoId", required = false) Long tipoId,                 
            Pageable pageable) {

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));

        return ResponseEntity.ok()
                .body(service.filtrarDocumentos(numeroFactura, nit, tipoId, sortedPageable));
    }

    // 👁️ 2. VISOR INLINE DE SOPORTES (¡Versión Segura Anti-Nulls!)
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewDocumento(@PathVariable("id") Long id) {
        Resource resource = service.descargarDocumento(id);
        
        // Evitamos el NullPointerException si el nombre de archivo viene vacío
        String filename = resource.getFilename();
        if (filename == null) {
            filename = "soporte_" + id;
        }
        
        // Detección dinámica y segura de la extensión
        String contentType;
        if (filename.toLowerCase().endsWith(".xml")) {
            contentType = MediaType.APPLICATION_XML_VALUE;
        } else if (filename.toLowerCase().endsWith(".pdf")) {
            contentType = MediaType.APPLICATION_PDF_VALUE;
        } else {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // 💾 3. DESCARGA UNITARIA DEL DOCUMENTO
    @GetMapping("/descargar/{id}")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable("id") Long id) {
        Resource resource = service.descargarDocumento(id);
        
        String filename = resource.getFilename();
        if (filename == null) {
            filename = "documento_" + id;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    // 📥 4. DESCARGA MASIVA FILTRADA (.ZIP)
    @PostMapping("/descarga-masiva")
    public ResponseEntity<byte[]> descargarMasivoZip(@RequestBody List<Long> ids) {
        ByteArrayOutputStream baos = service.generarZipMasivo(ids);
        byte[] zipBytes = baos.toByteArray();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"soportes_comprimidos_masivos.zip\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(zipBytes.length))
                .body(zipBytes);
    }
}