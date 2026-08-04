package com.gestor_fe.core.dto;

import lombok.Data;

@Data
public class GestionDto {
    private String estadoAccion;       // "APROBADO" o "RECHAZADO"
    private Long causalDevolucionId;  // ID del catálogo de causales
    private String observacion;        // Texto de la observación
    private Long tipoRegistroContableId;// FC, GV, ORC, NI, TB
    private String numeroCausacion; // No. de Causación
    private String usuario;
}