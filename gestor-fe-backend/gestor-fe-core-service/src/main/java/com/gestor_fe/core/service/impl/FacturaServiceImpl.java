package com.gestor_fe.core.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.client.AdminFeignClient;
import com.gestor_fe.core.dto.ConfiguracionFaseExtensionDto;
import com.gestor_fe.core.dto.ExtensionDto;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(FacturaServiceImpl.class);

    private final FacturaRepository repository;
    private final AdminFeignClient adminFeignClient;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${ruta.storage.validos}")
    private String rutaStorageValidos;

    public FacturaServiceImpl(FacturaRepository repository, AdminFeignClient adminFeignClient) {
        this.repository = repository;
        this.adminFeignClient = adminFeignClient;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Factura> findById(Long id) {
        return repository.findById(id);
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
            rol.equalsIgnoreCase("gestor-fe-reporte")
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

        if (filtro.getCodigoTipoRegistroContable() != null && !filtro.getCodigoTipoRegistroContable().isBlank()) {
            predicates.add(cb.equal(root.get("codigoTipoRegistroContable"), filtro.getCodigoTipoRegistroContable()));
        }

        if (filtro.getCodigoConcepto() != null && !filtro.getCodigoConcepto().isBlank()) {
            predicates.add(cb.like(cb.upper(root.get("codigoConcepto")), "%" + filtro.getCodigoConcepto().trim().toUpperCase() + "%"));
        }

        if (filtro.getCodigoMovimiento() != null && !filtro.getCodigoMovimiento().isBlank()) {
            predicates.add(cb.equal(root.get("codigoMovimiento"), filtro.getCodigoMovimiento()));
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
                    factura.setCodigoCausalDevolucion(null);
                    factura.setDeletedAt(null);
                } else {
                    factura.setEstado("ANULADO");
                    factura.setFaseId(1L);
                    factura.setCodigoCausalDevolucion(dto.getCodigoCausalDevolucion());
                    factura.setObservacion(dto.getObservacion());
                    factura.setDeletedAt(LocalDate.now());

                    gestion.setCodigoCausalDevolucion(dto.getCodigoCausalDevolucion());
                    gestion.setObservacion(dto.getObservacion());
                }
                break;

            case 2:
                if (esAprobado) {
                    factura.setEstado("CAUSADO");
                    factura.setCodigoTipoRegistroContable(dto.getCodigoTipoRegistroContable());
                    factura.setNumeroCausacion(dto.getNumeroCausacion());
                    factura.setFaseId(3L);
                    factura.setObservacion(null);
                    factura.setCodigoCausalDevolucion(null);

                    gestion.setCodigoTipoRegistroContable(dto.getCodigoTipoRegistroContable());
                    gestion.setNumeroCausacion(dto.getNumeroCausacion());
                } else {
                    factura.setEstado("RECHAZADO");
                    factura.setFaseId(1L);
                    factura.setCodigoCausalDevolucion(dto.getCodigoCausalDevolucion());
                    factura.setObservacion(dto.getObservacion());

                    gestion.setCodigoCausalDevolucion(dto.getCodigoCausalDevolucion());
                    gestion.setObservacion(dto.getObservacion());
                }
                break;

            case 3:
                if (esAprobado) {
                    factura.setEstado("IMPUESTOS VERIFICADOS");
                    factura.setFaseId(4L);
                    factura.setObservacion(null);
                    factura.setCodigoCausalDevolucion(null);
                } else {
                    factura.setEstado("RECHAZADO");
                    factura.setFaseId(2L);
                    factura.setCodigoCausalDevolucion(dto.getCodigoCausalDevolucion());
                    factura.setObservacion(dto.getObservacion());

                    gestion.setCodigoCausalDevolucion(dto.getCodigoCausalDevolucion());
                    gestion.setObservacion(dto.getObservacion());
                }
                break;

            case 4:
                if (esAprobado) {
                    factura.setEstado("PAGADO");
                    factura.setCodigoTipoRegistroContable(dto.getCodigoTipoRegistroContable());
                    factura.setFaseId(4L);
                    factura.setObservacion(null);
                    factura.setCodigoCausalDevolucion(null);

                    gestion.setCodigoTipoRegistroContable(dto.getCodigoTipoRegistroContable());
                    gestion.setNumeroCausacion(dto.getNumeroCausacion());
                } else {
                    factura.setEstado("RECHAZADO");
                    factura.setFaseId(3L);
                    factura.setCodigoCausalDevolucion(dto.getCodigoCausalDevolucion());
                    factura.setObservacion(dto.getObservacion());

                    gestion.setCodigoCausalDevolucion(dto.getCodigoCausalDevolucion());
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
    public Factura procesarCausacionFase2(Long id, String codigoTipoRegistroContable, String numeroCausacion, String usuario, MultipartFile archivoCausacion) {
        Factura factura = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con el ID: " + id));

        factura.setEstado("CAUSADO");
        factura.setCodigoTipoRegistroContable(codigoTipoRegistroContable);
        factura.setNumeroCausacion(numeroCausacion);
        factura.setFaseId(3L);
        factura.setObservacion(null);
        factura.setCodigoCausalDevolucion(null);

        // ⚡ ASIGNACIÓN RIGUROSA DE USUARIO
        String usuarioAccion = (usuario != null && !usuario.isBlank()) ? usuario.trim() : "SISTEMA";

        Gestion gestion = new Gestion();
        gestion.setFactura(factura);
        gestion.setFaseId(2L);
        gestion.setAccion("APROBADO");
        gestion.setEstadoResultado("CAUSADO");
        gestion.setCodigoTipoRegistroContable(codigoTipoRegistroContable);
        gestion.setNumeroCausacion(numeroCausacion);
        gestion.setUsuario(usuarioAccion); // 👈 Guardar el usuario en la auditoría

        factura.addGestion(gestion);

        if (archivoCausacion != null && !archivoCausacion.isEmpty()) {
            // 🛡️ VALIDACIÓN DINÁMICA DE EXTENSIÓN Y TAMAÑO SEGÚN CONFIGURACIÓN FASE 2
            String codigoExtension = validarArchivoSegunConfiguracionFase(archivoCausacion, 2L, "Soporte de Causación");

            try {
                // 🔄 Inactivar lógicamente cualquier soporte de causación previo activo
                if (factura.getDocumentos() != null) {
                    for (Documento doc : factura.getDocumentos()) {
                        if (doc.getDeletedAt() == null && doc.getCodigoTipo() != null && "8".equals(doc.getCodigoTipo()) && (doc.getRuta() == null || !doc.getRuta().contains("_TB_") && !doc.getRuta().contains("_PAGO_"))) {
                            doc.setDeletedAt(LocalDate.now());
                        }
                    }
                }

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
                docCausacion.setCodigoEstado("01");
                docCausacion.setCodigoExtension(codigoExtension != null ? codigoExtension : "02");
                docCausacion.setCodigoTipo("08");
                docCausacion.setFactura(factura);

                factura.addDocumento(docCausacion);

            } catch (IOException e) {
                throw new RuntimeException("Error al guardar físicamente el archivo de causación: " + e.getMessage(), e);
            }
        }

        return repository.save(factura);
    }

    @Override
    @Transactional
    public Factura procesarPagoFase4(Long id, String codigoTipoRegistroContable, String numeroCausacion, String usuario, MultipartFile soporteTb, MultipartFile comprobantePago) {
        Factura factura = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con el ID: " + id));

        factura.setEstado("PAGADO");
        if (codigoTipoRegistroContable != null && !codigoTipoRegistroContable.isBlank()) {
            factura.setCodigoTipoRegistroContable(codigoTipoRegistroContable);
        }
        if (numeroCausacion != null && !numeroCausacion.isBlank()) {
            factura.setNumeroCausacion(numeroCausacion);
        }
        factura.setFaseId(4L);
        factura.setObservacion(null);
        factura.setCodigoCausalDevolucion(null);

        // ⚡ ASIGNACIÓN RIGUROSA DE USUARIO
        String usuarioAccion = (usuario != null && !usuario.isBlank()) ? usuario.trim() : "SISTEMA";

        Gestion gestion = new Gestion();
        gestion.setFactura(factura);
        gestion.setFaseId(4L);
        gestion.setAccion("APROBADO");
        gestion.setEstadoResultado("PAGADO");
        gestion.setCodigoTipoRegistroContable(codigoTipoRegistroContable);
        gestion.setNumeroCausacion(numeroCausacion);
        gestion.setUsuario(usuarioAccion); // 👈 Guardar el usuario en la auditoría

        factura.addGestion(gestion);

        // 🛡️ VALIDACIÓN DINÁMICA DE EXTENSIÓN Y TAMAÑO SEGÚN CONFIGURACIÓN FASE 4
        String codigoExtensionTb = null;
        if (soporteTb != null && !soporteTb.isEmpty()) {
            codigoExtensionTb = validarArchivoSegunConfiguracionFase(soporteTb, 4L, "Documento Registro Contable TB");
        }

        String codigoExtensionPago = null;
        if (comprobantePago != null && !comprobantePago.isEmpty()) {
            codigoExtensionPago = validarArchivoSegunConfiguracionFase(comprobantePago, 4L, "Comprobante de Pago Bancario");
        }

        String nitCarpeta = factura.getNit().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        String numFacturaCarpeta = factura.getNumeroFactura().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        Path directorioFactura = Paths.get(rutaStorageValidos, nitCarpeta, numFacturaCarpeta);

        try {
            if (!Files.exists(directorioFactura)) {
                Files.createDirectories(directorioFactura);
            }

            if (soporteTb != null && !soporteTb.isEmpty()) {
                inactivarDocumentoPrevioPorPrefijo(factura, "_TB_");
                guardarSoporteDocumento(factura, soporteTb, directorioFactura, "TB_", "08", codigoExtensionTb);
            }

            if (comprobantePago != null && !comprobantePago.isEmpty()) {
                inactivarDocumentoPrevioPorPrefijo(factura, "_PAGO_");
                guardarSoporteDocumento(factura, comprobantePago, directorioFactura, "PAGO_", "08", codigoExtensionPago);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar los soportes de pago en disco: " + e.getMessage(), e);
        }

        return repository.save(factura);
    }

    /**
     * 🛡️ Valida que un archivo cumpla con las extensiones permitidas y el tamaño máximo en MB
     * configurado dinámicamente en el módulo de administración para la fase especificada.
     */
    private String validarArchivoSegunConfiguracionFase(MultipartFile archivo, Long faseId, String campoNombre) {
        if (archivo == null || archivo.isEmpty()) {
            return "02";
        }

        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            throw new IllegalArgumentException(String.format("El archivo '%s' no tiene una extensión válida.", nombreArchivo));
        }

        String extArchivo = nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase().trim();

        try {
            List<ConfiguracionFaseExtensionDto> configs = adminFeignClient.obtenerConfiguracionesPorFase(faseId);
            List<ExtensionDto> extensiones = adminFeignClient.listarExtensiones();

            if (configs != null && !configs.isEmpty()) {
                Map<Long, String> extMap = new HashMap<>();
                Map<Long, String> extCodMap = new HashMap<>();
                if (extensiones != null) {
                    for (ExtensionDto e : extensiones) {
                        if (e.getId() != null) {
                            String code = (e.getCodigo() != null && !e.getCodigo().isBlank()) ? e.getCodigo() : e.getDescripcion();
                            if (code != null) {
                                extMap.put(e.getId(), code.toLowerCase().replace(".", "").trim());
                                extCodMap.put(e.getId(), (e.getCodigo() != null && !e.getCodigo().isBlank()) ? e.getCodigo() : e.getDescripcion());
                            }
                        }
                    }
                }

                ConfiguracionFaseExtensionDto configCoincidente = null;
                String codigoExtensionCoincidente = null;

                for (ConfiguracionFaseExtensionDto c : configs) {
                    String extConfig = extMap.get(c.getExtensionId());
                    if (extConfig != null && extConfig.equalsIgnoreCase(extArchivo)) {
                        configCoincidente = c;
                        codigoExtensionCoincidente = extCodMap.get(c.getExtensionId());
                        break;
                    }
                }

                if (configCoincidente == null) {
                    List<String> permitidas = configs.stream()
                            .map(c -> extMap.getOrDefault(c.getExtensionId(), "ID " + c.getExtensionId()))
                            .map(s -> "." + s.toUpperCase())
                            .toList();
                    throw new IllegalArgumentException(String.format(
                            "Extensión no permitida para [%s] en la Fase %d. El archivo adjunto '%s' es .%s pero solo se permiten: %s",
                            campoNombre, faseId, nombreArchivo, extArchivo.toUpperCase(), String.join(", ", permitidas)
                    ));
                }

                int maxMb = (configCoincidente.getTamanoMaximoMb() != null && configCoincidente.getTamanoMaximoMb() > 0)
                        ? configCoincidente.getTamanoMaximoMb()
                        : 10;
                long maxBytes = (long) maxMb * 1024 * 1024;

                if (archivo.getSize() > maxBytes) {
                    double pesoRealMb = (double) archivo.getSize() / (1024 * 1024);
                    throw new IllegalArgumentException(String.format(
                            "El archivo '%s' para [%s] supera el límite de tamaño configurado para la Fase %d (Pesa %.2f MB, Máximo permitido: %d MB).",
                            nombreArchivo, campoNombre, faseId, pesoRealMb, maxMb
                    ));
                }

                return codigoExtensionCoincidente != null ? codigoExtensionCoincidente : extArchivo.toUpperCase();
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("⚠️ No se pudo consultar la parametrización vía Feign para Fase {}: {}", faseId, e.getMessage());
        }

        return "02";
    }

    private void inactivarDocumentoPrevioPorPrefijo(Factura factura, String prefijo) {
        if (factura.getDocumentos() != null) {
            for (Documento doc : factura.getDocumentos()) {
                if (doc.getDeletedAt() == null && doc.getRuta() != null && doc.getRuta().contains(prefijo)) {
                    doc.setDeletedAt(LocalDate.now());
                }
            }
        }
    }

    private void guardarSoporteDocumento(Factura factura, MultipartFile archivo, Path directorio, String prefijo, String codigoTipo, String codigoExtension) throws IOException {
        String nombreOriginal = archivo.getOriginalFilename();
        String nombreUnico = UUID.randomUUID() + "_" + prefijo + nombreOriginal;
        Path destinoFinal = directorio.resolve(nombreUnico);

        archivo.transferTo(destinoFinal.toFile());

        Documento doc = new Documento();
        doc.setNombreOriginal(nombreOriginal);
        doc.setRuta(destinoFinal.toString());
        doc.setTamano(archivo.getSize());
        doc.setCodigoEstado("01");
        doc.setCodigoExtension(codigoExtension != null ? codigoExtension : "02");
        doc.setCodigoTipo(codigoTipo);
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

    private static final Map<Long, String> NOMBRES_FASES_MAP = Map.of(
            1L, "Radicación",
            2L, "Reconocimiento Contable",
            3L, "Impuestos",
            4L, "Pendiente de Pago",
            5L, "Seguimiento",
            6L, "Finalizadas"
    );

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarFacturasExcel(String nitPrestador, List<String> rolesUsuario, FacturaFilterDto filtro) {
        if (filtro == null) {
            filtro = new FacturaFilterDto();
        }

        boolean esAdminOGestor = rolesUsuario != null && rolesUsuario.stream().anyMatch(rol ->
            rol.equalsIgnoreCase("admin") ||
            rol.equalsIgnoreCase("gestor-fe-admin") ||
            rol.equalsIgnoreCase("gestor-fe-f5-sf") ||
            rol.equalsIgnoreCase("gestor-fe-reporte")
        );

        if (!esAdminOGestor && nitPrestador != null && !nitPrestador.isBlank()) {
            filtro.setNit(nitPrestador);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Factura> query = cb.createQuery(Factura.class);
        Root<Factura> root = query.from(Factura.class);

        List<Predicate> predicates = construirPredicados(cb, root, filtro);
        query.where(cb.and(predicates.toArray(new Predicate[0])));
        query.orderBy(cb.desc(root.get("id")));

        List<Factura> facturas = entityManager.createQuery(query).getResultList();

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Trazabilidad Facturas");

            // Estilo para encabezados (Azul corporativo #1F4E78)
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            byte[] blueRgb = new byte[]{(byte) 31, (byte) 78, (byte) 120};
            headerStyle.setFillForegroundColor(new XSSFColor(blueRgb, new DefaultIndexedColorMap()));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Estilos para datos numéricos y de moneda
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0.00"));

            String[] encabezados = {
                "ID",
                "ESTADO",
                "ETAPA / FASE ACTUAL",
                "NIT EMISOR",
                "RAZÓN SOCIAL",
                "NO. FACTURA",
                "CUFE",
                "TIPO REGISTRO",
                "NO. CAUSACIÓN",
                "VALOR SUBTOTAL",
                "VALOR IVA",
                "VALOR TOTAL",
                "FECHA EMISIÓN",
                "OBSERVACIONES / CAUSAL",
                "FECHA RADICACIÓN"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < encabezados.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(encabezados[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Factura f : facturas) {
                Row row = sheet.createRow(rowIdx++);

                // ID
                row.createCell(0).setCellValue(f.getId() != null ? f.getId() : 0);

                // ESTADO
                row.createCell(1).setCellValue(f.getEstado() != null ? f.getEstado() : "");

                // ETAPA / FASE ACTUAL
                String nombreFase = f.getFaseId() != null 
                        ? NOMBRES_FASES_MAP.getOrDefault(f.getFaseId(), "Fase " + f.getFaseId()) 
                        : "Sin Fase";
                row.createCell(2).setCellValue(nombreFase);

                // NIT EMISOR
                row.createCell(3).setCellValue(f.getNit() != null ? f.getNit() : "");

                // RAZÓN SOCIAL
                row.createCell(4).setCellValue(f.getRazonSocialEmisor() != null ? f.getRazonSocialEmisor() : "");

                // NO. FACTURA
                row.createCell(5).setCellValue(f.getNumeroFactura() != null ? f.getNumeroFactura() : "");

                // CUFE
                row.createCell(6).setCellValue(f.getCufe() != null ? f.getCufe() : "");

                // TIPO REGISTRO
                row.createCell(7).setCellValue(f.getCodigoTipoRegistroContable() != null ? f.getCodigoTipoRegistroContable() : "");

                // NO. CAUSACIÓN
                row.createCell(8).setCellValue(f.getNumeroCausacion() != null ? f.getNumeroCausacion() : "");

                // VALOR SUBTOTAL
                Cell celdaSubtotal = row.createCell(9);
                BigDecimal subtotal = f.getValorSubtotal() != null ? f.getValorSubtotal() : BigDecimal.ZERO;
                celdaSubtotal.setCellValue(subtotal.doubleValue());
                celdaSubtotal.setCellStyle(currencyStyle);

                // VALOR IVA
                Cell celdaIva = row.createCell(10);
                BigDecimal iva = f.getValorIva() != null ? f.getValorIva() : BigDecimal.ZERO;
                celdaIva.setCellValue(iva.doubleValue());
                celdaIva.setCellStyle(currencyStyle);

                // VALOR TOTAL
                Cell celdaTotal = row.createCell(11);
                BigDecimal total = f.getValorTotal() != null ? f.getValorTotal() : BigDecimal.ZERO;
                celdaTotal.setCellValue(total.doubleValue());
                celdaTotal.setCellStyle(currencyStyle);

                // FECHA EMISIÓN
                row.createCell(12).setCellValue(f.getFechaEmision() != null ? f.getFechaEmision().toString() : "");

                // OBSERVACIONES / CAUSAL
                row.createCell(13).setCellValue(f.getObservacion() != null ? f.getObservacion() : "");

                // FECHA RADICACIÓN
                row.createCell(14).setCellValue(f.getCreatedAt() != null ? f.getCreatedAt().toString() : "");
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                workbook.write(baos);
                workbook.dispose();
                return baos.toByteArray();
            }
        } catch (IOException e) {
            LOGGER.error("❌ Error generando archivo Excel de trazabilidad", e);
            throw new RuntimeException("Error generando archivo Excel de trazabilidad", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarFormato1001Excel(String nitPrestador, List<String> rolesUsuario, FacturaFilterDto filtro) {
        if (filtro == null) {
            filtro = new FacturaFilterDto();
        }

        boolean esAdminOGestor = rolesUsuario != null && rolesUsuario.stream().anyMatch(rol ->
            rol.equalsIgnoreCase("admin") ||
            rol.equalsIgnoreCase("gestor-fe-admin") ||
            rol.equalsIgnoreCase("gestor-fe-f5-sf") ||
            rol.equalsIgnoreCase("gestor-fe-reporte")
        );

        if (!esAdminOGestor && nitPrestador != null && !nitPrestador.isBlank()) {
            filtro.setNit(nitPrestador);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Factura> query = cb.createQuery(Factura.class);
        Root<Factura> root = query.from(Factura.class);

        List<Predicate> predicates = construirPredicados(cb, root, filtro);
        query.where(cb.and(predicates.toArray(new Predicate[0])));
        query.orderBy(cb.desc(root.get("id")));

        List<Factura> facturas = entityManager.createQuery(query).getResultList();

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Formato 1001 DIAN");

            // Estilo para encabezados (Azul corporativo #1F4E78 como en la imagen)
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            byte[] blueRgb = new byte[]{(byte) 31, (byte) 78, (byte) 120};
            headerStyle.setFillForegroundColor(new XSSFColor(blueRgb, new DefaultIndexedColorMap()));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Estilos para datos numéricos y de moneda
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0.00"));

            String[] encabezados = {
                "concepto",
                "tipo_docto",
                "numero_identificacion",
                "primer_Apellido",
                "segundo_apellido",
                "primer_nombre",
                "segundo_nombre",
                "razon_social",
                "direccion",
                "codigo_departamento",
                "codigo_municipio",
                "codigo_pais",
                "pago_abono_en_cuenta_gto deducible",
                "apgo_abono_en_cuenta_no_deducible",
                "iva_mayor_vr_gasto",
                "iva_mayor_vr_del_cosot_gasto_no_deducible",
                "retencion_en_la_fuente",
                "fte_asumida",
                "retfte_iva",
                "fte_iva_no_residentes"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < encabezados.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(encabezados[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Factura f : facturas) {
                Row row = sheet.createRow(rowIdx++);

                // 1. concepto
                row.createCell(0).setCellValue(f.getCodigoConcepto() != null ? f.getCodigoConcepto() : "");

                // 2. tipo_docto
                String tipoDocId = f.getCodigoTipoIdentificacionEmisor();
                if ((tipoDocId == null || tipoDocId.isBlank()) && f.getNit() != null && !f.getNit().isBlank()) {
                    tipoDocId = "31";
                }
                row.createCell(1).setCellValue(tipoDocId != null ? tipoDocId : "");

                // 3. numero_identificacion
                row.createCell(2).setCellValue(f.getNit() != null ? f.getNit() : "");

                // 4. primer_Apellido
                row.createCell(3).setCellValue(f.getPrimerApellido() != null ? f.getPrimerApellido() : "");

                // 5. segundo_apellido
                row.createCell(4).setCellValue(f.getSegundoApellido() != null ? f.getSegundoApellido() : "");

                // 6. primer_nombre
                row.createCell(5).setCellValue(f.getPrimerNombre() != null ? f.getPrimerNombre() : "");

                // 7. segundo_nombre
                row.createCell(6).setCellValue(f.getSegundoNombre() != null ? f.getSegundoNombre() : "");

                // 8. razon_social
                row.createCell(7).setCellValue(f.getRazonSocialEmisor() != null ? f.getRazonSocialEmisor() : "");

                // 9. direccion
                row.createCell(8).setCellValue(f.getDireccion() != null ? f.getDireccion() : "");

                // 10. codigo_departamento
                row.createCell(9).setCellValue(f.getCodigoDepartamento() != null ? f.getCodigoDepartamento() : "");

                // 11. codigo_municipio
                String codMpio = f.getCodigoMunicipio();
                if (codMpio != null && codMpio.length() > 3) {
                    codMpio = codMpio.substring(codMpio.length() - 3);
                }
                row.createCell(10).setCellValue(codMpio != null ? codMpio : "");

                // 12. codigo_pais
                String codPais = f.getCodigoPais();
                if (codPais == null || codPais.isBlank() || "CO".equalsIgnoreCase(codPais) || "COL".equalsIgnoreCase(codPais)) {
                    codPais = "169";
                }
                row.createCell(11).setCellValue(codPais);

                // 13. pago_abono_en_cuenta_gto deducible
                Cell celdaGtoDeducible = row.createCell(12);
                BigDecimal gastoDeducible = f.getValorSubtotal() != null ? f.getValorSubtotal() : (f.getValorTotal() != null ? f.getValorTotal() : BigDecimal.ZERO);
                celdaGtoDeducible.setCellValue(gastoDeducible.doubleValue());
                celdaGtoDeducible.setCellStyle(currencyStyle);

                // 14. apgo_abono_en_cuenta_no_deducible
                Cell celdaGtoNoDeducible = row.createCell(13);
                celdaGtoNoDeducible.setCellValue(0.00);
                celdaGtoNoDeducible.setCellStyle(currencyStyle);

                // 15. iva_mayor_vr_gasto
                Cell celdaIvaGasto = row.createCell(14);
                BigDecimal iva = f.getValorIva() != null ? f.getValorIva() : BigDecimal.ZERO;
                celdaIvaGasto.setCellValue(iva.doubleValue());
                celdaIvaGasto.setCellStyle(currencyStyle);

                // 16. iva_mayor_vr_del_cosot_gasto_no_deducible
                Cell celdaIvaNoDeducible = row.createCell(15);
                celdaIvaNoDeducible.setCellValue(0.00);
                celdaIvaNoDeducible.setCellStyle(currencyStyle);

                // 17. retencion_en_la_fuente (se deja vacío según solicitud)
                Cell celdaReteFuente = row.createCell(16);
                celdaReteFuente.setCellValue("");

                // 18. fte_asumida
                Cell celdaFteAsumida = row.createCell(17);
                celdaFteAsumida.setCellValue("");

                // 19. retfte_iva
                Cell celdaRetfteIva = row.createCell(18);
                if (f.getValorReteiva() != null) {
                    celdaRetfteIva.setCellValue(f.getValorReteiva().doubleValue());
                    celdaRetfteIva.setCellStyle(currencyStyle);
                } else {
                    celdaRetfteIva.setCellValue("");
                }

                // 20. fte_iva_no_residentes
                Cell celdaFteIvaNoRes = row.createCell(19);
                celdaFteIvaNoRes.setCellValue("");
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                workbook.write(baos);
                workbook.dispose();
                return baos.toByteArray();
            }
        } catch (IOException e) {
            LOGGER.error("❌ Error generando archivo Excel de Formato 1001 DIAN", e);
            throw new RuntimeException("Error generando archivo Excel de Formato 1001 DIAN", e);
        }
    }

    @Override
    @Transactional
    public Factura asignarConcepto(Long id, String codigoConcepto, String usuario) {
        Factura factura = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la factura con ID: " + id));

        String anterior = factura.getCodigoConcepto();
        factura.setCodigoConcepto(codigoConcepto != null && !codigoConcepto.isBlank() ? codigoConcepto.trim() : null);

        Gestion gestion = new Gestion();
        gestion.setFactura(factura);
        gestion.setUsuario(usuario != null && !usuario.isBlank() ? usuario : "SISTEMA");
        gestion.setAccion("ASIGNAR_CONCEPTO");
        gestion.setObservacion("Asignación de Concepto Contable / DIAN: " 
                + (anterior != null ? "[" + anterior + " -> " : "[") 
                + (factura.getCodigoConcepto() != null ? factura.getCodigoConcepto() : "Sin concepto") + "]");
        gestion.setFaseId(factura.getFaseId());
        gestion.setEstadoResultado(factura.getEstado() != null ? factura.getEstado() : "EN GESTIÓN");
        factura.addGestion(gestion);

        LOGGER.info("🏷️ Factura ID {} asignada con Concepto: {}", id, factura.getCodigoConcepto());
        return repository.save(factura);
    }
}