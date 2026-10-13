package com.gestor_fe.core.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestor_fe.core.dto.EstadisticasDto.*;
import com.gestor_fe.core.service.EstadisticaService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class EstadisticaServiceImpl implements EstadisticaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EstadisticaServiceImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    private static final Map<Long, String> NOMBRES_FASES_DEFAULT = Map.of(
            1L, "1. Radicación",
            2L, "2. Reconocimiento Contable",
            3L, "3. Impuestos",
            4L, "4. Pendiente de Pago",
            5L, "5. Seguimiento",
            6L, "6. Finalizadas"
    );

    @Override
    @Transactional(readOnly = true)
    public ResumenResponseDto obtenerResumenEstadisticas(
            String nitFiltro,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String usuario,
            List<String> roles
    ) {
        boolean esSoloPrestador = false;
        boolean esAdmin = false;

        if (roles != null && !roles.isEmpty()) {
            esAdmin = roles.stream().anyMatch(r ->
                    r.equalsIgnoreCase("admin") ||
                    r.equalsIgnoreCase("gestor-fe-admin")
            );

            boolean esAdminOGestor = esAdmin || roles.stream().anyMatch(r ->
                    r.equalsIgnoreCase("gestor-fe-f1-g") ||
                    r.equalsIgnoreCase("gestor-fe-f2-rc") ||
                    r.equalsIgnoreCase("gestor-fe-f3-i") ||
                    r.equalsIgnoreCase("gestor-fe-f4-pp") ||
                    r.equalsIgnoreCase("gestor-fe-f5-sf")
            );
            esSoloPrestador = !esAdminOGestor && roles.stream().anyMatch(r -> r.equalsIgnoreCase("gestor-fe-prestador"));
        }

        // Si es rol prestador exclusivo, forzamos su NIT/usuario
        String nitEfectivo = nitFiltro;
        if (esSoloPrestador && usuario != null && !usuario.isBlank() && !usuario.equalsIgnoreCase("GESTOR_SISTEMA")) {
            nitEfectivo = usuario.trim();
        }

        StringBuilder whereClause = new StringBuilder(" WHERE f.deleted_at IS NULL ");
        Map<String, Object> params = new HashMap<>();

        if (nitEfectivo != null && !nitEfectivo.isBlank()) {
            whereClause.append(" AND f.nit = :nit ");
            params.put("nit", nitEfectivo.trim());
        }

        if (fechaInicio != null) {
            whereClause.append(" AND CAST(COALESCE(f.fecha_emision, f.created_at) AS date) >= :fechaInicio ");
            params.put("fechaInicio", fechaInicio);
        }

        if (fechaFin != null) {
            whereClause.append(" AND CAST(COALESCE(f.fecha_emision, f.created_at) AS date) <= :fechaFin ");
            params.put("fechaFin", fechaFin);
        }

        // 1. CONSULTA DE KPIS GENERALES
        KpisDto kpis = consultarKpis(whereClause.toString(), params);

        // 2. CONSULTA POR FASE
        List<FaseDto> porFase = consultarPorFase(whereClause.toString(), params, kpis.getTotalFacturas());

        // 3. CONSULTA POR ESTADO
        List<EstadoDto> porEstado = consultarPorEstado(whereClause.toString(), params, kpis.getTotalFacturas());

        // 4. CONSULTA TOP PRESTADORES
        List<PrestadorDto> topPrestadores = consultarTopPrestadores(whereClause.toString(), params);

        // 5. CONSULTA GESTIONES POR USUARIO (Para Administradores)
        List<UsuarioGestionDto> gestionesPorUsuario = new ArrayList<>();
        if (esAdmin || !esSoloPrestador) {
            gestionesPorUsuario = consultarGestionesPorUsuario(fechaInicio, fechaFin);
        }

        return ResumenResponseDto.builder()
                .kpis(kpis)
                .porFase(porFase)
                .porEstado(porEstado)
                .topPrestadores(topPrestadores)
                .gestionesPorUsuario(gestionesPorUsuario)
                .build();
    }

    private KpisDto consultarKpis(String where, Map<String, Object> params) {
        String sql = "SELECT " +
                " COUNT(f.id) as total_facturas, " +
                " COALESCE(SUM(f.valor_total), 0) as monto_total, " +
                " COUNT(CASE WHEN UPPER(COALESCE(f.estado, '')) IN ('APROBADO', 'PAGADO', 'CAUSADO', 'VALIDADO') THEN 1 END) as facturas_aprobadas, " +
                " COALESCE(SUM(CASE WHEN UPPER(COALESCE(f.estado, '')) IN ('APROBADO', 'PAGADO', 'CAUSADO', 'VALIDADO') THEN f.valor_total ELSE 0 END), 0) as monto_aprobadas, " +
                " COUNT(CASE WHEN UPPER(COALESCE(f.estado, '')) IN ('RECHAZADO', 'ANULADO', 'FACTURA NO CONFORME', 'CON ERRORES', 'DEVOLVER FACTURA ELECTRÓNICA') THEN 1 END) as facturas_rechazadas, " +
                " COALESCE(SUM(CASE WHEN UPPER(COALESCE(f.estado, '')) IN ('RECHAZADO', 'ANULADO', 'FACTURA NO CONFORME', 'CON ERRORES', 'DEVOLVER FACTURA ELECTRÓNICA') THEN f.valor_total ELSE 0 END), 0) as monto_rechazadas, " +
                " COUNT(CASE WHEN UPPER(COALESCE(f.estado, '')) NOT IN ('APROBADO', 'PAGADO', 'CAUSADO', 'VALIDADO', 'RECHAZADO', 'ANULADO', 'FACTURA NO CONFORME', 'CON ERRORES', 'DEVOLVER FACTURA ELECTRÓNICA') THEN 1 END) as facturas_tramite, " +
                " COALESCE(SUM(CASE WHEN UPPER(COALESCE(f.estado, '')) NOT IN ('APROBADO', 'PAGADO', 'CAUSADO', 'VALIDADO', 'RECHAZADO', 'ANULADO', 'FACTURA NO CONFORME', 'CON ERRORES', 'DEVOLVER FACTURA ELECTRÓNICA') THEN f.valor_total ELSE 0 END), 0) as monto_tramite, " +
                " COUNT(DISTINCT f.nit) as total_prestadores " +
                " FROM gestor.factura f " + where;

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);

        Object[] row = (Object[]) query.getSingleResult();

        long totalFacturas = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        BigDecimal montoTotal = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
        long facturasAprobadas = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        BigDecimal montoAprobadas = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
        long facturasRechazadas = row[4] != null ? ((Number) row[4]).longValue() : 0L;
        BigDecimal montoRechazadas = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;
        long facturasTramite = row[6] != null ? ((Number) row[6]).longValue() : 0L;
        BigDecimal montoTramite = row[7] != null ? new BigDecimal(row[7].toString()) : BigDecimal.ZERO;
        long totalPrestadores = row[8] != null ? ((Number) row[8]).longValue() : 0L;

        return KpisDto.builder()
                .totalFacturas(totalFacturas)
                .montoTotal(montoTotal)
                .facturasEnTramite(facturasTramite)
                .montoEnTramite(montoTramite)
                .facturasAprobadas(facturasAprobadas)
                .montoAprobadas(montoAprobadas)
                .facturasRechazadas(facturasRechazadas)
                .montoRechazadas(montoRechazadas)
                .totalPrestadores(totalPrestadores)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<FaseDto> consultarPorFase(String where, Map<String, Object> params, long totalGeneral) {
        String sql = "SELECT " +
                " COALESCE(f.fase_id, 1) as fase_id, " +
                " COUNT(f.id) as cantidad, " +
                " COALESCE(SUM(f.valor_total), 0) as monto " +
                " FROM gestor.factura f " + where +
                " GROUP BY COALESCE(f.fase_id, 1) " +
                " ORDER BY fase_id ASC";

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);

        List<Object[]> rows = query.getResultList();
        List<FaseDto> resultado = new ArrayList<>();

        for (Object[] r : rows) {
            Long faseId = r[0] != null ? ((Number) r[0]).longValue() : 1L;
            long cantidad = r[1] != null ? ((Number) r[1]).longValue() : 0L;
            BigDecimal monto = r[2] != null ? new BigDecimal(r[2].toString()) : BigDecimal.ZERO;

            double porcentaje = totalGeneral > 0
                    ? BigDecimal.valueOf((cantidad * 100.0) / totalGeneral).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            String faseNombre = NOMBRES_FASES_DEFAULT.getOrDefault(faseId, "Fase " + faseId);

            resultado.add(FaseDto.builder()
                    .faseId(faseId)
                    .faseNombre(faseNombre)
                    .cantidad(cantidad)
                    .montoTotal(monto)
                    .porcentaje(porcentaje)
                    .build());
        }

        return resultado;
    }

    @SuppressWarnings("unchecked")
    private List<EstadoDto> consultarPorEstado(String where, Map<String, Object> params, long totalGeneral) {
        String sql = "SELECT " +
                " COALESCE(NULLIF(TRIM(f.estado), ''), 'SIN ESTADO') as estado, " +
                " COUNT(f.id) as cantidad " +
                " FROM gestor.factura f " + where +
                " GROUP BY COALESCE(NULLIF(TRIM(f.estado), ''), 'SIN ESTADO') " +
                " ORDER BY cantidad DESC";

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);

        List<Object[]> rows = query.getResultList();
        List<EstadoDto> resultado = new ArrayList<>();

        for (Object[] r : rows) {
            String estado = r[0] != null ? r[0].toString() : "DESCONOCIDO";
            long cantidad = r[1] != null ? ((Number) r[1]).longValue() : 0L;

            double porcentaje = totalGeneral > 0
                    ? BigDecimal.valueOf((cantidad * 100.0) / totalGeneral).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            resultado.add(EstadoDto.builder()
                    .estado(estado)
                    .cantidad(cantidad)
                    .porcentaje(porcentaje)
                    .build());
        }

        return resultado;
    }

    @SuppressWarnings("unchecked")
    private List<PrestadorDto> consultarTopPrestadores(String where, Map<String, Object> params) {
        String sql = "SELECT " +
                " f.nit, " +
                " MAX(COALESCE(f.razon_social_emisor, 'DESCONOCIDO')) as razon_social, " +
                " COUNT(f.id) as total_facturas, " +
                " COALESCE(SUM(f.valor_total), 0) as monto_total, " +
                " COUNT(CASE WHEN UPPER(COALESCE(f.estado, '')) IN ('APROBADO', 'PAGADO', 'CAUSADO', 'VALIDADO') THEN 1 END) as aprobadas, " +
                " COUNT(CASE WHEN UPPER(COALESCE(f.estado, '')) IN ('RECHAZADO', 'ANULADO', 'FACTURA NO CONFORME', 'CON ERRORES', 'DEVOLVER FACTURA ELECTRÓNICA') THEN 1 END) as rechazadas, " +
                " COUNT(CASE WHEN UPPER(COALESCE(f.estado, '')) NOT IN ('APROBADO', 'PAGADO', 'CAUSADO', 'VALIDADO', 'RECHAZADO', 'ANULADO', 'FACTURA NO CONFORME', 'CON ERRORES', 'DEVOLVER FACTURA ELECTRÓNICA') THEN 1 END) as pendientes " +
                " FROM gestor.factura f " + where +
                " GROUP BY f.nit " +
                " ORDER BY total_facturas DESC " +
                " LIMIT 10";

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);

        List<Object[]> rows = query.getResultList();
        List<PrestadorDto> resultado = new ArrayList<>();

        for (Object[] r : rows) {
            String nit = r[0] != null ? r[0].toString() : "";
            String razonSocial = r[1] != null ? r[1].toString() : "DESCONOCIDO";
            long total = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            BigDecimal monto = r[3] != null ? new BigDecimal(r[3].toString()) : BigDecimal.ZERO;
            long aprobadas = r[4] != null ? ((Number) r[4]).longValue() : 0L;
            long rechazadas = r[5] != null ? ((Number) r[5]).longValue() : 0L;
            long pendientes = r[6] != null ? ((Number) r[6]).longValue() : 0L;

            resultado.add(PrestadorDto.builder()
                    .nit(nit)
                    .razonSocial(razonSocial)
                    .totalFacturas(total)
                    .montoTotal(monto)
                    .facturasAprobadas(aprobadas)
                    .facturasRechazadas(rechazadas)
                    .facturasPendientes(pendientes)
                    .build());
        }

        return resultado;
    }

    @SuppressWarnings("unchecked")
    private List<UsuarioGestionDto> consultarGestionesPorUsuario(LocalDate fechaInicio, LocalDate fechaFin) {
        StringBuilder sql = new StringBuilder("SELECT " +
                " COALESCE(NULLIF(TRIM(g.usuario), ''), 'DESCONOCIDO') as usuario, " +
                " COUNT(g.id) as total_gestiones, " +
                " COUNT(CASE WHEN UPPER(COALESCE(g.accion, '')) = 'APROBADO' THEN 1 END) as aprobadas, " +
                " COUNT(CASE WHEN UPPER(COALESCE(g.accion, '')) IN ('RECHAZADO', 'ANULADO', 'DEVOLUCION', 'FACTURA NO CONFORME') THEN 1 END) as rechazadas " +
                " FROM gestor.gestion g " +
                " WHERE g.deleted_at IS NULL ");

        Map<String, Object> params = new HashMap<>();
        if (fechaInicio != null) {
            sql.append(" AND CAST(g.created_at AS date) >= :fechaInicioG ");
            params.put("fechaInicioG", fechaInicio);
        }
        if (fechaFin != null) {
            sql.append(" AND CAST(g.created_at AS date) <= :fechaFinG ");
            params.put("fechaFinG", fechaFin);
        }

        sql.append(" GROUP BY COALESCE(NULLIF(TRIM(g.usuario), ''), 'DESCONOCIDO') " +
                " ORDER BY total_gestiones DESC LIMIT 10");

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        List<Object[]> rows = query.getResultList();
        List<UsuarioGestionDto> resultado = new ArrayList<>();

        for (Object[] r : rows) {
            String usuario = r[0] != null ? r[0].toString() : "DESCONOCIDO";
            long total = r[1] != null ? ((Number) r[1]).longValue() : 0L;
            long aprobadas = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            long rechazadas = r[3] != null ? ((Number) r[3]).longValue() : 0L;

            double pct = total > 0
                    ? BigDecimal.valueOf((aprobadas * 100.0) / total).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            resultado.add(UsuarioGestionDto.builder()
                    .usuario(usuario)
                    .totalGestiones(total)
                    .aprobadas(aprobadas)
                    .rechazadas(rechazadas)
                    .porcentajeEfectividad(pct)
                    .build());
        }

        return resultado;
    }
}
