package com.gestor_fe.core.dto;

import java.io.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaZipWrapperDto {
    
    private String nombreBase; // El nombre del archivo sin extensión (ej: "FACT-2520")
    private File archivoXml;   // Referencia al archivo XML extraído en la carpeta temporal
    private File archivoPdf;   // Referencia al archivo PDF extraído (puede ser null si no viene en el ZIP)
    
}