package com.gestor_fe.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import java.io.ByteArrayOutputStream;
import java.util.List;

import com.gestor_fe.core.entity.Documento;

public interface DocumentoService {

    Page<Documento> findByDeletedAtIsNull(Pageable pageable);
    
    // Método para filtrar documentos de forma dinámica y paginada
    Page<Documento> filtrarDocumentos(String numeroFactura, String nit, Long tipoId, Pageable pageable);
    
    // Método para recuperar un documento físico del disco por su ID
    Resource descargarDocumento(Long id);
    
    // Método para generar un ZIP en memoria a partir de una lista de IDs de documentos
    ByteArrayOutputStream generarZipMasivo(List<Long> ids);
}