package com.gestor_fe.core.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class FacturaFilterDto {

    private String nit;                     // Filtro por NIT emisor (Coincidencia parcial)
    private String numeroFactura;           // Filtro por número de factura
    private String cufe;                    // Filtro por CUFE / UUID
    private String razonSocialEmisor;       // Filtro por razón social emisor
    private String estado;                  // Filtro por estado actual
    private Long faseId;                    // Filtro específico por Fase/Etapa (1, 2, 3, 4, 5)
    private String numeroCausacion;         // Número de documento de causación
    private Long tipoRegistroContableId;    // FC, GV, ORC, NI
    private LocalDate fechaEmisionDesde;    // Rango fecha emisión desde
    private LocalDate fechaEmisionHasta;    // Rango fecha emisión hasta
    private BigDecimal valorTotalMin;       // Rango de valor mínimo
    private BigDecimal valorTotalMax;       // Rango de valor máximo
    private String textoBusquedaGlobal;     // Búsqueda general multitermino (OR entre campos clave)
}