package com.gestor_fe.core.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.repository.DocumentoRepository;
import com.gestor_fe.core.service.DocumentoService;

@Service
public class DocumentoServiceImpl implements DocumentoService {

    private final DocumentoRepository repository;
    
    public DocumentoServiceImpl(DocumentoRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public Page<Documento> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    // 🚀 BÚSQUEDA AVANZADA COMBINADA Y PAGINADA CON CRITERIA API
    @Override
    public Page<Documento> filtrarDocumentos(String numeroFactura, String nit, Long tipoId, Pageable pageable) {
        Specification<Documento> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 1. Filtro base: Que el documento no esté eliminado lógicamente
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            
            // 2. Filtro por Tipo de Documento (XML, PDF, etc.)
            if (tipoId != null && tipoId > 0) {
                predicates.add(criteriaBuilder.equal(root.get("tipoId"), tipoId));
            }
            
            // 3. Filtros avanzados cruzados por Factura (Número o NIT)
            if ((numeroFactura != null && !numeroFactura.trim().isEmpty()) || (nit != null && !nit.trim().isEmpty())) {
                
                // Creamos una subconsulta para obtener los Documentos que pertenecen a Facturas que coincidan con los filtros
                Subquery<Documento> subquery = query.subquery(Documento.class);
                Root<Factura> facturaRoot = subquery.from(Factura.class);
                
                // Hacemos el join dentro de la subconsulta usando la lista de 'documentos' que sí existe en Factura
                Join<Factura, Documento> documentosJoin = facturaRoot.join("documentos");
                
                List<Predicate> subqueryPredicates = new ArrayList<>();
                
                // Filtro por número de factura
                if (numeroFactura != null && !numeroFactura.trim().isEmpty()) {
                    subqueryPredicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(facturaRoot.get("numeroFactura")), 
                        "%" + numeroFactura.toLowerCase().trim() + "%"
                    ));
                }
                
                // Filtro por NIT
                if (nit != null && !nit.trim().isEmpty()) {
                    subqueryPredicates.add(criteriaBuilder.equal(facturaRoot.get("nit"), nit.trim()));
                }
                
                // Seleccionamos los documentos de esas facturas en la subconsulta
                subquery.select(documentosJoin);
                subquery.where(criteriaBuilder.and(subqueryPredicates.toArray(new Predicate[0])));
                
                // Finalmente, obligamos a que el Documento principal esté dentro de los resultados de esa subconsulta
                predicates.add(root.in(subquery));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return repository.findAll(spec, pageable);
    }

    // 📄 OBTENCIÓN FÍSICA DEL ARCHIVO (Mismo método sirve para el visor inline y descarga)
    @Override
    public Resource descargarDocumento(Long id) {
        // 1. Validar que el registro exista en la base de datos
        Documento documento = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado con el ID: " + id));
        
        // 2. Validar si está eliminado lógicamente
        if (documento.getDeletedAt() != null) {
            throw new RuntimeException("El documento solicitado se encuentra eliminado lógicamente.");
        }

        try {
            // 3. Convertir la cadena de texto de la base de datos a una ruta física (Path)
            Path path = Paths.get(documento.getRuta());
            
            // 4. ¡VALIDACIÓN CLAVE!: Comprobar si el archivo realmente existe en el disco duro del servidor
            if (!java.nio.file.Files.exists(path)) {
                throw new RuntimeException("Error de consistencia: El archivo físico NO existe en la ruta: " + documento.getRuta());
            }
            
            // 5. Crear el recurso para la descarga/visualización
            Resource resource = new UrlResource(path.toUri());
            
            // 6. Validar que el recurso sea legible
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("El archivo existe en el disco pero el sistema no tiene permisos para leerlo.");
            }
            
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error crítico en el formato de la URL de almacenamiento: " + e.getMessage());
        }
    }

    // 📥 DESCARGA COMPRIMIDA MASIVA DINÁMICA DE SOPORTES EN UN ÚNICO .ZIP
    @Override
    public ByteArrayOutputStream generarZipMasivo(List<Long> ids) {
        List<Documento> documentos = repository.findAllById(ids);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            byte[] buffer = new byte[4096];
            
            for (Documento doc : documentos) {
                if (doc.getDeletedAt() != null) continue; // Ignora los eliminados

                Path path = Paths.get(doc.getRuta());
                java.io.File file = path.toFile();
                
                if (!file.exists()) {
                    System.err.println("Archivo físico faltante ignorado en el ZIP: " + doc.getRuta());
                    continue;
                }

                try (FileInputStream fis = new FileInputStream(file)) {
                    // Evitamos colisiones de nombres usando el ID como prefijo único del soporte
                    String zipEntryName = doc.getId() + "_" + doc.getNombreOriginal();
                    zos.putNextEntry(new ZipEntry(zipEntryName));
                    
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
            }
            zos.finish();
        } catch (IOException e) {
            throw new RuntimeException("Error crítico generando el empaquetado masivo ZIP: " + e.getMessage());
        }
        
        return baos;
    }
}