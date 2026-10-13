package com.gestor_fe.core.dto;

import lombok.Data;

@Data
public class GestionDto {
    private String estadoAccion;       // "APROBADO" o "RECHAZADO"
    private String codigoCausalDevolucion; // Código del catálogo de causales (String)
    private String observacion;        // Texto de la observación
    private String codigoTipoRegistroContable; // FC, GV, ORC, NI, TB (String)
    private String numeroCausacion; // No. de Causación
    private String usuario;
}