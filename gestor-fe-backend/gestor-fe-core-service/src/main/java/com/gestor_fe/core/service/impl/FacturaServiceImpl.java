package com.gestor_fe.core.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.dto.FacturaFilterDto;
import com.gestor_fe.core.dto.GestionDto;
import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.entity.Gestion;
import com.gestor_fe.core.repository.FacturaRepository;
import com.gestor_fe.core.service.FacturaService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Service
public class FacturaServiceImpl implements FacturaService {

    private final FacturaRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${ruta.storage.validos}")
    private String rutaStorageValidos;

    public FacturaServiceImpl(FacturaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> findByNitAndDeletedAtIsNull(String nit, Pageable pageable) {
        return repository.findByNitAndDeletedAtIsNull(nit, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> findByFaseIdAndDeletedAtIsNull(Long faseId, Pageable pageable) {
        return repository.findByFaseIdAndDeletedAtIsNull(faseId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> findByFaseActiva(Long faseId, Pageable pageable) {
        if (faseId != null && faseId == 4L) {
            return repository.findByFaseCuatroPendientePago(faseId, pageable);
        }
        return repository.findByFaseActiva(faseId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> findByDeletedAtIsNull(Pageable pageable) {
        return repository.findByDeletedAtIsNull(pageable);
    }

    // =========================================================================
    // 🔍 BÚSQUEDA CRITERIA
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> buscarConCriteria(FacturaFilterDto filtro, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Factura> query = cb.createQuery(Factura.class);
        Root<Factura> root = query.from(Factura.class);

        List<Predicate> predicates = construirPredicados(cb, root, filtro);
        query.where(predicates.toArray(new Predicate[0]));

        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(sortOrder -> {
                if (sortOrder.isAscending()) {
                    orders.add(cb.asc(root.get(sortOrder.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(sortOrder.getProperty())));
                }
            });
            query.orderBy(orders);
        } else {
            query.orderBy(cb.desc(root.get("id")));
        }

        TypedQuery<Factura> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Factura> facturas = typedQuery.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Factura> countRoot = countQuery.from(Factura.class);

        List<Predicate> countPredicates = construirPredicados(cb, countRoot, filtro);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(facturas, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> buscarTrazabilidadSegunRol(String nitPrestador, List<String> rolesUsuario, FacturaFilterDto filtro, Pageable pageable) {
        if (filtro == null) {
            filtro = new FacturaFilterDto();
        }

        boolean esAdminOGestor = rolesUsuario != null && rolesUsuario.stream().anyMatch(rol ->
            rol.equalsIgnoreCase("admin") ||
            rol.equalsIgnoreCase("gestor-fe-admin") ||
            rol.equalsIgnoreCase("gestor-fe-f5-sf") ||
            rol.equalsIgnoreCase("default-roles-fe")
        );

        if (!esAdminOGestor && nitPrestador != null && !nitPrestador.isBlank()) {
            filtro.setNit(nitPrestador);
        }

        return buscarConCriteria(filtro, pageable);
    }

    private List<Predicate> construirPredicados(CriteriaBuilder cb, Root<Factura> root, FacturaFilterDto filtro) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isNull(root.get("deletedAt")));

        if (filtro == null) {
            return predicates;
        }

        if (filtro.getFaseId() != null && filtro.getFaseId() == 4L) {
            if (filtro.getEstado() == null || filtro.getEstado().isBlank()) {
                predicates.add(cb.notEqual(cb.upper(root.get("estado")), "PAGADO"));
            }
        }

        if (filtro.getNit() != null && !filtro.getNit().isBlank()) {
            predicates.add(cb.like(cb.upper(root.get("nit")), "%" + filtro.getNit().trim().toUpperCase() + "%"));
        }

        if (filtro.getNumeroFactura() != null && !filtro.getNumeroFactura().isBlank()) {
            predicates.add(cb.like(cb.upper(root.get("numeroFactura")), "%" + filtro.getNumeroFactura().trim().toUpperCase() + "%"));
        }

        if (filtro.getCufe() != null && !filtro.getCufe().isBlank()) {
            predicates.add(cb.like(cb.upper(root.get("cufe")), "%" + filtro.getCufe().trim().toUpperCase() + "%"));
        }

        if (filtro.getRazonSocialEmisor() != null && !filtro.getRazonSocialEmisor().isBlank()) {
            predicates.add(cb.like(cb.upper(root.get("razonSocialEmisor")), "%" + filtro.getRazonSocialEmisor().trim().toUpperCase() + "%"));
        }

        if (filtro.getEstado() != null && !filtro.getEstado().isBlank()) {
            predicates.add(cb.equal(cb.upper(root.get("estado")), filtro.getEstado().trim().toUpperCase()));
        }

        if (filtro.getFaseId() != null) {
            predicates.add(cb.equal(root.get("faseId"), filtro.getFaseId()));
        }

        if (filtro.getNumeroCausacion() != null && !filtro.getNumeroCausacion().isBlank()) {
            predicates.add(cb.like(cb.upper(root.get("numeroCausacion")), "%" + filtro.getNumeroCausacion().trim().toUpperCase() + "%"));
        }

        if (filtro.getTipoRegistroContableId() != null) {
            predicates.add(cb.equal(root.get("tipoRegistroContableId"), filtro.getTipoRegistroContableId()));
        }

        if (filtro.getFechaEmisionDesde() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), filtro.getFechaEmisionDesde()));
        }

        if (filtro.getFechaEmisionHasta() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("fechaEmision"), filtro.getFechaEmisionHasta()));
        }

        if (filtro.getValorTotalMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("valorTotal"), filtro.getValorTotalMin()));
        }

        if (filtro.getValorTotalMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("valorTotal"), filtro.getValorTotalMax()));
        }

        if (filtro.getTextoBusquedaGlobal() != null && !filtro.getTextoBusquedaGlobal().isBlank()) {
            String term = "%" + filtro.getTextoBusquedaGlobal().trim().toUpperCase() + "%";
            Predicate globalSearch = cb.or(
                cb.like(cb.upper(root.get("nit")), term),
                cb.like(cb.upper(root.get("numeroFactura")), term),
                cb.like(cb.upper(root.get("razonSocialEmisor")), term),
                cb.like(cb.upper(root.get("cufe")), term),
                cb.like(cb.upper(root.get("numeroCausacion")), term)
            );
            predicates.add(globalSearch);
        }

        return predicates;
    }

    // =========================================================================
    // ⚙️ MOTOR UNIFICADO DE TRANSICIÓN DE FASES CON GUARDADO DE HISTORIAL Y USUARIO
    // =========================================================================
    @Override
    @Transactional
    public Factura procesarTransicionFase(Long id, Long faseActualId, GestionDto dto) {
        Factura factura = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con el ID: " + id));

        boolean esAprobado = "APROBADO".equalsIgnoreCase(dto.getEstadoAccion());
        int fase = faseActualId != null ? faseActualId.intValue() : 1;

        // ⚡ VALIDACIÓN Y ASIGNACIÓN RIGUROSA DEL USUARIO
        String usuarioAccion = (dto != null && dto.getUsuario() != null && !dto.getUsuario().isBlank()) 
                               ? dto.getUsuario().trim() 
                               : "SISTEMA";

        Gestion gestion = new Gestion();
        gestion.setFactura(factura);
        gestion.setFaseId(faseActualId);
        gestion.setAccion(dto.getEstadoAccion());
        gestion.setUsuario(usuarioAccion); // 👈 Asigna el usuario a la entidad Gestion

        switch (fase) {
            case 1:
                if (esAprobado) {
                    factura.setEstado("EN GESTIÓN");
                    factura.setFaseId(2L);
                    factura.setObservacion(null);
                    factura.setCausalDevolucionId(null);
                    factura.setDeletedAt(null);
                } else {
                    factura.setEstado("ANULADO");
                    factura.setFaseId(1L);
                    factura.setCausalDevolucionId(dto.getCausalDevolucionId());
                    factura.setObservacion(dto.getObservacion());
                    factura.setDeletedAt(LocalDate.now());

                    gestion.setCausalDevolucionId(dto.getCausalDevolucionId());
                    gestion.setObservacion(dto.getObservacion());
                }
                break;

            case 2:
                if (esAprobado) {
                    factura.setEstado("CAUSADO");
                    factura.setTipoRegistroContableId(dto.getTipoRegistroContableId());
                    factura.setNumeroCausacion(dto.getNumeroCausacion());
                    factura.setFaseId(3L);
                    factura.setObservacion(null);
                    factura.setCausalDevolucionId(null);

                    gestion.setTipoRegistroContableId(dto.getTipoRegistroContableId());
                    gestion.setNumeroCausacion(dto.getNumeroCausacion());
                } else {
                    factura.setEstado("RECHAZADO");
                    factura.setFaseId(1L);
                    factura.setCausalDevolucionId(dto.getCausalDevolucionId());
                    factura.setObservacion(dto.getObservacion());

                    gestion.setCausalDevolucionId(dto.getCausalDevolucionId());
                    gestion.setObservacion(dto.getObservacion());
                }
                break;

            case 3:
                if (esAprobado) {
                    factura.setEstado("IMPUESTOS VERIFICADOS");
                    factura.setFaseId(4L);
                    factura.setObservacion(null);
                    factura.setCausalDevolucionId(null);
                } else {
                    factura.setEstado("RECHAZADO");
                    factura.setFaseId(2L);
                    factura.setCausalDevolucionId(dto.getCausalDevolucionId());
                    factura.setObservacion(dto.getObservacion());

                    gestion.setCausalDevolucionId(dto.getCausalDevolucionId());
                    gestion.setObservacion(dto.getObservacion());
                }
                break;

            case 4:
                if (esAprobado) {
                    factura.setEstado("PAGADO");
                    factura.setTipoRegistroContableId(dto.getTipoRegistroContableId());
                    factura.setFaseId(4L);
                    factura.setObservacion(null);
                    factura.setCausalDevolucionId(null);

                    gestion.setTipoRegistroContableId(dto.getTipoRegistroContableId());
                    gestion.setNumeroCausacion(dto.getNumeroCausacion());
                } else {
                    factura.setEstado("RECHAZADO");
                    factura.setFaseId(3L);
                    factura.setCausalDevolucionId(dto.getCausalDevolucionId());
                    factura.setObservacion(dto.getObservacion());

                    gestion.setCausalDevolucionId(dto.getCausalDevolucionId());
                    gestion.setObservacion(dto.getObservacion());
                }
                break;

            default:
                throw new IllegalArgumentException("La fase proporcionada no es válida: " + faseActualId);
        }

        gestion.setEstadoResultado(factura.getEstado());
        factura.addGestion(gestion);

        return repository.save(factura);
    }

    // =========================================================================
    // ⚙️ CAUSACIÓN (FASE 2) Y PAGO (FASE 4) CON GUARDADO DE USUARIO OBLIGATORIO
    // =========================================================================

    @Override
    @Transactional
    public Factura procesarCausacionFase2(Long id, Long tipoRegistroContableId, String numeroCausacion, String usuario, MultipartFile archivoCausacion) {
        Factura factura = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con el ID: " + id));

        factura.setEstado("CAUSADO");
        factura.setTipoRegistroContableId(tipoRegistroContableId);
        factura.setNumeroCausacion(numeroCausacion);
        factura.setFaseId(3L);
        factura.setObservacion(null);
        factura.setCausalDevolucionId(null);

        // ⚡ ASIGNACIÓN RIGUROSA DE USUARIO
        String usuarioAccion = (usuario != null && !usuario.isBlank()) ? usuario.trim() : "SISTEMA";

        Gestion gestion = new Gestion();
        gestion.setFactura(factura);
        gestion.setFaseId(2L);
        gestion.setAccion("APROBADO");
        gestion.setEstadoResultado("CAUSADO");
        gestion.setTipoRegistroContableId(tipoRegistroContableId);
        gestion.setNumeroCausacion(numeroCausacion);
        gestion.setUsuario(usuarioAccion); // 👈 CORRECCIÓN CRÍTICA: Se guarda el usuario en la auditoría

        factura.addGestion(gestion);

        if (archivoCausacion != null && !archivoCausacion.isEmpty()) {
            try {
                String nitCarpeta = factura.getNit().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                String numFacturaCarpeta = factura.getNumeroFactura().replaceAll("[\\\\/:*?\"<>|]", "_").trim();

                Path directorioFactura = Paths.get(rutaStorageValidos, nitCarpeta, numFacturaCarpeta);
                if (!Files.exists(directorioFactura)) {
                    Files.createDirectories(directorioFactura);
                }

                String nombreOriginal = archivoCausacion.getOriginalFilename();
                String nombreUnico = UUID.randomUUID() + "_causacion_" + nombreOriginal;
                Path destinoFinal = directorioFactura.resolve(nombreUnico);

                archivoCausacion.transferTo(destinoFinal.toFile());

                Documento docCausacion = new Documento();
                docCausacion.setNombreOriginal(nombreOriginal);
                docCausacion.setRuta(destinoFinal.toString());
                docCausacion.setTamano(archivoCausacion.getSize());
                docCausacion.setEstadoId(1L);
                docCausacion.setExtensionId(1L);
                docCausacion.setTipoId(8L);
                docCausacion.setFactura(factura);

                factura.addDocumento(docCausacion);

            } catch (IOException e) {
                throw new RuntimeException("Error al guardar físicamente el PDF de causación: " + e.getMessage(), e);
            }
        }

        return repository.save(factura);
    }

    @Override
    @Transactional
    public Factura procesarPagoFase4(Long id, Long tipoRegistroContableId, String numeroCausacion, String usuario, MultipartFile soporteTb, MultipartFile comprobantePago) {
        Factura factura = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con el ID: " + id));

        factura.setEstado("PAGADO");
        if (tipoRegistroContableId != null) {
            factura.setTipoRegistroContableId(tipoRegistroContableId);
        }
        if (numeroCausacion != null && !numeroCausacion.isBlank()) {
            factura.setNumeroCausacion(numeroCausacion);
        }
        factura.setFaseId(4L);
        factura.setObservacion(null);
        factura.setCausalDevolucionId(null);

        // ⚡ ASIGNACIÓN RIGUROSA DE USUARIO
        String usuarioAccion = (usuario != null && !usuario.isBlank()) ? usuario.trim() : "SISTEMA";

        Gestion gestion = new Gestion();
        gestion.setFactura(factura);
        gestion.setFaseId(4L);
        gestion.setAccion("APROBADO");
        gestion.setEstadoResultado("PAGADO");
        gestion.setTipoRegistroContableId(tipoRegistroContableId);
        gestion.setNumeroCausacion(numeroCausacion);
        gestion.setUsuario(usuarioAccion); // 👈 CORRECCIÓN CRÍTICA: Se guarda el usuario en la auditoría

        factura.addGestion(gestion);

        String nitCarpeta = factura.getNit().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        String numFacturaCarpeta = factura.getNumeroFactura().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        Path directorioFactura = Paths.get(rutaStorageValidos, nitCarpeta, numFacturaCarpeta);

        try {
            if (!Files.exists(directorioFactura)) {
                Files.createDirectories(directorioFactura);
            }

            if (soporteTb != null && !soporteTb.isEmpty()) {
                guardarSoporteDocumento(factura, soporteTb, directorioFactura, "TB_", 8L);
            }

            if (comprobantePago != null && !comprobantePago.isEmpty()) {
                guardarSoporteDocumento(factura, comprobantePago, directorioFactura, "PAGO_", 8L);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar los soportes de pago en disco: " + e.getMessage(), e);
        }

        return repository.save(factura);
    }

    private void guardarSoporteDocumento(Factura factura, MultipartFile archivo, Path directorio, String prefijo, Long tipoId) throws IOException {
        String nombreOriginal = archivo.getOriginalFilename();
        String nombreUnico = UUID.randomUUID() + "_" + prefijo + nombreOriginal;
        Path destinoFinal = directorio.resolve(nombreUnico);

        archivo.transferTo(destinoFinal.toFile());

        Documento doc = new Documento();
        doc.setNombreOriginal(nombreOriginal);
        doc.setRuta(destinoFinal.toString());
        doc.setTamano(archivo.getSize());
        doc.setEstadoId(1L);
        doc.setExtensionId(1L);
        doc.setTipoId(tipoId);
        doc.setFactura(factura);

        factura.addDocumento(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findExistingCufes(List<String> cufes) {
        if (cufes == null || cufes.isEmpty()) {
            return List.of();
        }
        return repository.findExistingCufes(cufes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findExistingNitFacturas(List<String> nitFacturas) {
        if (nitFacturas == null || nitFacturas.isEmpty()) {
            return List.of();
        }
        return repository.findExistingNitFacturas(nitFacturas);
    }
}