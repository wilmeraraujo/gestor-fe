package com.gestor_fe.core.service.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Prestador;
import com.gestor_fe.core.repository.DocumentoRepository;
import com.gestor_fe.core.repository.PrestadorRepository;
import com.gestor_fe.core.service.PrestadorService;

import jakarta.persistence.criteria.Predicate;

@Service
public class PrestadorServiceImpl implements PrestadorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrestadorServiceImpl.class);

    private final PrestadorRepository prestadorRepository;
    private final DocumentoRepository documentoRepository;
    private final String rutaStorageValidos;

    public PrestadorServiceImpl(
            PrestadorRepository prestadorRepository,
            DocumentoRepository documentoRepository,
            @Value("${ruta.storage.validos}") String rutaStorageValidos) {
        this.prestadorRepository = prestadorRepository;
        this.documentoRepository = documentoRepository;
        this.rutaStorageValidos = rutaStorageValidos;
    }

    // =========================================================================
    // 👤 MAESTRO PRESTADOR & SUBMÓDULO DE ADMINISTRACIÓN
    // =========================================================================

    @Override
    @Transactional
    public Prestador crearOActualizarPrestador(Prestador prestador) {
        return prestadorRepository.save(prestador);
    }

    @Override
    @Transactional
    public Prestador crear(Prestador prestador) {
        if (prestador.getNit() == null || prestador.getNit().isBlank()) {
            throw new IllegalArgumentException("El NIT del prestador es obligatorio.");
        }
        if (prestador.getRazonSocial() == null || prestador.getRazonSocial().isBlank()) {
            throw new IllegalArgumentException("La Razón Social del prestador es obligatoria.");
        }

        prestador.setNit(prestador.getNit().trim());
        prestador.setRazonSocial(prestador.getRazonSocial().trim());
        if (prestador.getDireccion() != null) prestador.setDireccion(prestador.getDireccion().trim());
        if (prestador.getTelefono() != null) prestador.setTelefono(prestador.getTelefono().trim());
        if (prestador.getEmail() != null) prestador.setEmail(prestador.getEmail().trim());

        if (prestador.getIdentificadorCargue() == null) {
            prestador.setIdentificadorCargue(0L);
        }

        prestador.setCreatedAt(LocalDateTime.now());
        prestador.setDeletedAt(null);

        return prestadorRepository.save(prestador);
    }

    @Override
    @Transactional
    public Prestador editar(Long id, Prestador prestador) {
        Prestador existente = prestadorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el prestador con ID: " + id));

        if (prestador.getNit() != null && !prestador.getNit().isBlank()) {
            existente.setNit(prestador.getNit().trim());
        }
        if (prestador.getRazonSocial() != null && !prestador.getRazonSocial().isBlank()) {
            existente.setRazonSocial(prestador.getRazonSocial().trim());
        }
        if (prestador.getDireccion() != null) {
            existente.setDireccion(prestador.getDireccion().trim());
        }
        if (prestador.getTelefono() != null) {
            existente.setTelefono(prestador.getTelefono().trim());
        }
        if (prestador.getEmail() != null) {
            existente.setEmail(prestador.getEmail().trim());
        }
        if (prestador.getIdentificadorCargue() != null) {
            existente.setIdentificadorCargue(prestador.getIdentificadorCargue());
        }

        existente.setUpdatedAt(LocalDateTime.now());

        return prestadorRepository.save(existente);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prestador> obtenerPorNit(String nit) {
        return prestadorRepository.findByNitAndDeletedAtIsNull(nit);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prestador> obtenerPorId(Long id) {
        return prestadorRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Prestador> listarPrestadores(Pageable pageable) {
        return prestadorRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Prestador> findByDeletedAtIsNull(Pageable pageable) {
        return prestadorRepository.findByDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prestador> findByDescripcion(String desc) {
        if (desc == null || desc.isBlank()) {
            return prestadorRepository.findAll();
        }
        return prestadorRepository.buscarPorTexto(desc.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Prestador> buscarPaginado(Map<String, String> filtros, Pageable pageable) {
        Specification<Prestador> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filtros != null) {
                filtros.forEach((key, val) -> {
                    if (val != null && !val.trim().isEmpty() && !key.equals("page") && !key.equals("size") && !key.equals("sort")) {
                        String valorLimpio = val.trim().toLowerCase();
                        switch (key) {
                            case "id":
                                try {
                                    predicates.add(cb.equal(root.get("id"), Long.valueOf(valorLimpio)));
                                } catch (NumberFormatException ignored) {}
                                break;
                            case "nit":
                                predicates.add(cb.like(cb.lower(root.get("nit")), "%" + valorLimpio + "%"));
                                break;
                            case "razonSocial":
                            case "descripcion":
                                predicates.add(cb.like(cb.lower(root.get("razonSocial")), "%" + valorLimpio + "%"));
                                break;
                            case "direccion":
                                predicates.add(cb.like(cb.lower(root.get("direccion")), "%" + valorLimpio + "%"));
                                break;
                            case "telefono":
                                predicates.add(cb.like(cb.lower(root.get("telefono")), "%" + valorLimpio + "%"));
                                break;
                            case "email":
                                predicates.add(cb.like(cb.lower(root.get("email")), "%" + valorLimpio + "%"));
                                break;
                            case "identificadorCargue":
                            case "codigo":
                                try {
                                    predicates.add(cb.equal(root.get("identificadorCargue"), Long.valueOf(valorLimpio)));
                                } catch (NumberFormatException ignored) {}
                                break;
                            case "estadoActivo":
                            case "estado":
                                if (valorLimpio.equalsIgnoreCase("activo") || valorLimpio.equals("true") || valorLimpio.equals("1")) {
                                    predicates.add(cb.isNull(root.get("deletedAt")));
                                } else if (valorLimpio.equalsIgnoreCase("inactivo") || valorLimpio.equals("false") || valorLimpio.equals("0")) {
                                    predicates.add(cb.isNotNull(root.get("deletedAt")));
                                }
                                break;
                            case "global":
                            case "buscar":
                                Predicate pNit = cb.like(cb.lower(root.get("nit")), "%" + valorLimpio + "%");
                                Predicate pRazon = cb.like(cb.lower(root.get("razonSocial")), "%" + valorLimpio + "%");
                                Predicate pEmail = cb.like(cb.lower(root.get("email")), "%" + valorLimpio + "%");
                                Predicate pTel = cb.like(cb.lower(root.get("telefono")), "%" + valorLimpio + "%");
                                Predicate pDir = cb.like(cb.lower(root.get("direccion")), "%" + valorLimpio + "%");
                                predicates.add(cb.or(pNit, pRazon, pEmail, pTel, pDir));
                                break;
                            default:
                                break;
                        }
                    }
                });
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return prestadorRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public Prestador toggleEstado(Long id) {
        Prestador p = prestadorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el prestador con ID: " + id));

        if (p.getDeletedAt() == null) {
            p.setDeletedAt(LocalDateTime.now());
        } else {
            p.setDeletedAt(null);
        }
        p.setUpdatedAt(LocalDateTime.now());
        return prestadorRepository.save(p);
    }

    @Override
    @Transactional
    public Prestador softDelete(Long id) {
        Prestador p = prestadorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el prestador con ID: " + id));

        p.setDeletedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return prestadorRepository.save(p);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        softDelete(id);
    }

    // =========================================================================
    // 📎 GESTIÓN DE SOPORTES (DOCUMENTOS DEL PRESTADOR)
    // =========================================================================

    @Override
    @Transactional
    public Documento cargarSoporte(String nitPrestador, String codigoTipo, String codigoExtension, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo cargado se encuentra vacío.");
        }

        // 1. Obtener prestador activo por NIT
        Prestador prestador = prestadorRepository.findByNitAndDeletedAtIsNull(nitPrestador)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un prestador activo con el NIT: " + nitPrestador));

        try {
            // 2. Si ya existe un documento activo del mismo tipo (ej. un RUT viejo), realizar Soft-Delete
            Optional<Documento> soporteExistente = documentoRepository
                    .findByPrestadorIdAndCodigoTipoAndDeletedAtIsNull(prestador.getId(), codigoTipo);

            soporteExistente.ifPresent(docOld -> {
                docOld.setDeletedAt(LocalDate.now());
                documentoRepository.save(docOld);
                LOGGER.info("ℹ️ Reemplazando soporte previo ID {} para el codigoTipo {}", docOld.getId(), codigoTipo);
            });

            // 3. Crear estructura física de directorios: E:\gestion-fe-validos\{NIT}\SOPORTES_PRESTADOR\
            String nitSaneado = sanearNombreCarpeta(nitPrestador);
            Path directorioSoportes = Paths.get(rutaStorageValidos, nitSaneado, "SOPORTES_PRESTADOR");

            if (!Files.exists(directorioSoportes)) {
                Files.createDirectories(directorioSoportes);
            }

            // 4. Copiar el archivo recibido a la ruta final con UUID único
            String nombreOriginal = StringUtils.cleanPath(
                    archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "soporte.pdf"
            );
            String nombreUnico = UUID.randomUUID() + "_" + nombreOriginal;
            Path destinoFinal = directorioSoportes.resolve(nombreUnico);

            Files.copy(archivo.getInputStream(), destinoFinal, StandardCopyOption.REPLACE_EXISTING);

            // 5. Instanciar la entidad Documento y mapear sus campos
            Documento documento = new Documento();
            documento.setNombreOriginal(nombreOriginal);
            documento.setRuta(destinoFinal.toString());
            documento.setTamano(archivo.getSize());
            documento.setCodigoEstado("01");
            
            String lowerNom = nombreOriginal.toLowerCase();
            if (codigoExtension != null && !codigoExtension.isBlank()) {
                if (codigoExtension.equalsIgnoreCase("PDF") || codigoExtension.equals("2")) {
                    documento.setCodigoExtension("02");
                } else if (codigoExtension.equalsIgnoreCase("XML") || codigoExtension.equals("1")) {
                    documento.setCodigoExtension("01");
                } else if (codigoExtension.equalsIgnoreCase("ZIP") || codigoExtension.equals("3")) {
                    documento.setCodigoExtension("03");
                } else {
                    documento.setCodigoExtension(codigoExtension);
                }
            } else if (lowerNom.endsWith(".xml")) {
                documento.setCodigoExtension("01");
            } else if (lowerNom.endsWith(".pdf")) {
                documento.setCodigoExtension("02");
            } else if (lowerNom.endsWith(".zip")) {
                documento.setCodigoExtension("03");
            } else {
                documento.setCodigoExtension("02");
            }
            documento.setCodigoTipo(codigoTipo);

            // Vinculación bidireccional usando el método helper de la entidad Prestador
            prestador.addSoporte(documento);

            Documento soporteGuardado = documentoRepository.save(documento);
            LOGGER.info("✅ Soporte guardado exitosamente con ID {} para el Prestador NIT {}", soporteGuardado.getId(), nitPrestador);

            return soporteGuardado;

        } catch (Exception e) {
            LOGGER.error("❌ Error guardando el soporte para el prestador {}: {}", nitPrestador, e.getMessage());
            throw new RuntimeException("No se pudo procesar y almacenar el archivo de soporte.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Documento> listarSoportes(Long prestadorId, Pageable pageable) {
        return documentoRepository.findByPrestadorIdAndDeletedAtIsNull(prestadorId, pageable);
    }

    @Override
    @Transactional
    public void eliminarSoporte(Long documentoId) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el documento con ID: " + documentoId));

        // Soft Delete en base de datos
        doc.setDeletedAt(LocalDate.now());
        documentoRepository.save(doc);

        // Intento opcional de limpieza en disco
        try {
            File archivoFisico = new File(doc.getRuta());
            if (archivoFisico.exists()) {
                archivoFisico.delete();
            }
        } catch (Exception e) {
            LOGGER.warn("⚠️ No se pudo eliminar el archivo físico en ruta {}: {}", doc.getRuta(), e.getMessage());
        }
    }

    private String sanearNombreCarpeta(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "DESCONOCIDO";
        }
        return nombre.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}