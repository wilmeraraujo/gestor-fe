package com.gestor_fe.core.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class EstadisticasDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpisDto {
        private long totalFacturas;
        private BigDecimal montoTotal;
        private long facturasEnTramite;
        private BigDecimal montoEnTramite;
        private long facturasAprobadas;
        private BigDecimal montoAprobadas;
        private long facturasRechazadas;
        private BigDecimal montoRechazadas;
        private long totalPrestadores;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaseDto {
        private Long faseId;
        private String faseNombre;
        private long cantidad;
        private BigDecimal montoTotal;
        private double porcentaje;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadoDto {
        private String estado;
        private long cantidad;
        private double porcentaje;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrestadorDto {
        private String nit;
        private String razonSocial;
        private long totalFacturas;
        private BigDecimal montoTotal;
        private long facturasAprobadas;
        private long facturasRechazadas;
        private long facturasPendientes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioGestionDto {
        private String usuario;
        private long totalGestiones;
        private long aprobadas;
        private long rechazadas;
        private double porcentajeEfectividad;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenResponseDto {
        private KpisDto kpis;
        private List<FaseDto> porFase;
        private List<EstadoDto> porEstado;
        private List<PrestadorDto> topPrestadores;
        private List<UsuarioGestionDto> gestionesPorUsuario;
    }
}
