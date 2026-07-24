package com.gestor_fe.core.step;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.w3c.dom.Document;

import com.gestor_fe.core.dto.FacturaZipWrapperDto;
import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.ErrorCargue;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.service.ErrorCargueService;
import com.gestor_fe.core.service.FacturaService;

public class FacturaZipProcessor implements ItemProcessor<FacturaZipWrapperDto, Factura> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacturaZipProcessor.class);

    private final Long identificadorCargue;
    private final FacturaService facturaService;
    private final ErrorCargueService errorCargueService;
    
    private long currentLine = 0;

    // 🧠 Caché en memoria durante la ejecución del Job para detectar repetidos DENTRO del mismo ZIP
    private final Set<String> cufesInBatch = new HashSet<>();
    private final Set<String> nitFacturasInBatch = new HashSet<>();

    public FacturaZipProcessor(Long identificadorCargue, 
                               FacturaService facturaService, 
                               ErrorCargueService errorCargueService) {
        this.identificadorCargue = identificadorCargue;
        this.facturaService = facturaService;
        this.errorCargueService = errorCargueService;
    }

    @Override
    public Factura process(FacturaZipWrapperDto item) throws Exception {
        currentLine++;
        
        File xmlFile = item.getArchivoXml();
        if (xmlFile == null || !xmlFile.exists()) {
            return null;
        }

        // 1. Extraer datos del XML
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(false);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        XPath xPath = XPathFactory.newInstance().newXPath();

        String nitEmisor = (String) xPath.compile("//SenderParty//CompanyID/text()").evaluate(doc, XPathConstants.STRING);
        String razonSocial = (String) xPath.compile("//SenderParty//RegistrationName/text()").evaluate(doc, XPathConstants.STRING);
        String numeroFactura = (String) xPath.compile("//ParentDocumentID/text()").evaluate(doc, XPathConstants.STRING);
        String cufe = (String) xPath.compile("//UUID/text()").evaluate(doc, XPathConstants.STRING);
        String fechaStr = (String) xPath.compile("//IssueDate/text()").evaluate(doc, XPathConstants.STRING);
        String valorStr = (String) xPath.compile("//LegalMonetaryTotal/PayableAmount/text()").evaluate(doc, XPathConstants.STRING);

        // ===================================================================================
        // ⚡ FASE 1: VALIDACIONES PRIMARIAS (LOCALES Y ESTRUCTURALES DEL ARCHIVO / ZIP)
        // ===================================================================================

        // 1.1 Verificación de etiquetas obligatorias no vacías en el XML
        if (nitEmisor == null || nitEmisor.isBlank()) {
            registrarError("CAMPO_OBLIGATORIO_VACIO", "NIT_EMISOR", 
                String.format("El archivo XML [%s] no contiene la etiqueta del NIT del emisor.", xmlFile.getName()), xmlFile.getName());
        }

        if (numeroFactura == null || numeroFactura.isBlank()) {
            registrarError("CAMPO_OBLIGATORIO_VACIO", "NUMERO_FACTURA", 
                String.format("El archivo XML [%s] no contiene la etiqueta del número de factura.", xmlFile.getName()), xmlFile.getName());
        }

        if (cufe == null || cufe.isBlank()) {
            registrarError("CAMPO_OBLIGATORIO_VACIO", "CUFE", 
                String.format("El archivo XML [%s] no contiene la etiqueta del CUFE / UUID.", xmlFile.getName()), xmlFile.getName());
        }

        String nitClean = nitEmisor.trim();
        String numFacturaClean = numeroFactura.trim();
        String cufeClean = cufe.trim();
        String llaveNitFactura = nitClean + "_" + numFacturaClean;

        // 1.2 Verificación de duplicidad interna dentro del propio archivo ZIP (en memoria RAM)
        if (cufesInBatch.contains(cufeClean)) {
            registrarError("CUFE_DUPLICADO_ZIP", "CUFE", 
                String.format("El CUFE [%s] está duplicado dentro del mismo archivo ZIP cargado.", cufeClean), cufeClean);
        }
        
        if (nitFacturasInBatch.contains(llaveNitFactura)) {
            registrarError("FACTURA_DUPLICADA_ZIP", "NIT_NUMERO_FACTURA", 
                String.format("La combinación NIT [%s] y Factura [%s] está duplicada dentro del mismo archivo ZIP.", nitClean, numFacturaClean), llaveNitFactura);
        }

        // ===================================================================================
        // 🗄️ FASE 2: VALIDACIONES SECUNDARIAS (PREEXISTENCIA EN LA BASE DE DATOS)
        // Se ejecutan ÚNICAMENTE si el archivo superó todas las validaciones primarias de la Fase 1
        // ===================================================================================
        
        // 2.1 Validar existencia de CUFE en PostgreSQL
        List<String> cufesExistentesBD = facturaService.findExistingCufes(List.of(cufeClean));
        if (!cufesExistentesBD.isEmpty()) {
            registrarError("CUFE_EXISTENTE_BD", "CUFE", 
                String.format("El CUFE [%s] ya existe registrado previamente en la base de datos.", cufeClean), cufeClean);
        }

        // 2.2 Validar existencia de NIT + Número de Factura en PostgreSQL
        List<String> nitFacturasExistentesBD = facturaService.findExistingNitFacturas(List.of(llaveNitFactura));
        if (!nitFacturasExistentesBD.isEmpty()) {
            registrarError("FACTURA_EXISTENTE_BD", "NIT_NUMERO_FACTURA", 
                String.format("La factura número [%s] del NIT [%s] ya existe en la base de datos.", numFacturaClean, nitClean), llaveNitFactura);
        }

        // Si pasó las dos fases exitosamente, guardamos en la memoria RAM del lote actual
        cufesInBatch.add(cufeClean);
        nitFacturasInBatch.add(llaveNitFactura);

        // Mapear objeto Factura
        Factura factura = new Factura();
        factura.setNit(nitClean);
        factura.setRazonSocialEmisor(razonSocial != null ? razonSocial.trim() : "DESCONOCIDO");
        factura.setNumeroFactura(numFacturaClean);
        factura.setCufe(cufeClean);
        factura.setIdentificadorCargue(identificadorCargue);
        factura.setLinea(currentLine);

        if (fechaStr != null && !fechaStr.isBlank()) {
            factura.setFechaEmision(LocalDate.parse(fechaStr.trim()));
        }
        if (valorStr != null && !valorStr.isBlank()) {
            factura.setValorTotal(new BigDecimal(valorStr.trim()));
        }
        
        factura.setEstado("RADICADO");
        factura.setFaseId(1L);
        factura.setObservacion("");

        // Mapear XML
        Documento docXml = new Documento();
        docXml.setNombreOriginal(xmlFile.getName());
        docXml.setTamano(xmlFile.length());
        docXml.setEstadoId(1L);    
        docXml.setExtensionId(1L); 
        docXml.setTipoId(1L);      
        docXml.setArchivoTemporal(xmlFile);
        factura.addDocumento(docXml);

        // Mapear PDF
        if (item.getArchivoPdf() != null && item.getArchivoPdf().exists()) {
            Documento docPdf = new Documento();
            docPdf.setNombreOriginal(item.getArchivoPdf().getName());
            docPdf.setTamano(item.getArchivoPdf().length());
            docPdf.setEstadoId(1L);
            docPdf.setExtensionId(2L); 
            docPdf.setTipoId(2L);
            docPdf.setArchivoTemporal(item.getArchivoPdf());
            factura.addDocumento(docPdf);
        }

        return factura;
    }

    private void registrarError(String tipoError, String campo, String descripcion, String valor) {
        ErrorCargue error = new ErrorCargue();
        error.setCargueId(identificadorCargue);
        error.setNumeroLinea((int) currentLine);
        error.setTipoError(tipoError);
        error.setCampo(campo);
        error.setError(descripcion);
        error.setValorAsociado(valor);
        error.setCreatedAt(LocalDateTime.now());
        
        // Persistir en tabla independiente de auditoría de errores
        errorCargueService.saveAll(List.of(error));
        
        LOGGER.error("❌ ERROR DETECTADO: {}", descripcion);
        throw new IllegalStateException(descripcion); // Corta la ejecución de inmediato
    }
}