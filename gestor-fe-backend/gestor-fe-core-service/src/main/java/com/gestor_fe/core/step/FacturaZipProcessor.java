package com.gestor_fe.core.step;

import java.io.File;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.gestor_fe.core.client.AdminFeignClient;
import com.gestor_fe.core.dto.FacturaZipWrapperDto;
import com.gestor_fe.core.dto.TipoDto;
import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.ErrorCargue;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.repository.DocumentoRepository;
import com.gestor_fe.core.service.ErrorCargueService;
import com.gestor_fe.core.service.FacturaService;

public class FacturaZipProcessor implements ItemProcessor<FacturaZipWrapperDto, Factura> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacturaZipProcessor.class);

    private final Long identificadorCargue;
    private final FacturaService facturaService;
    private final ErrorCargueService errorCargueService;
    private final DocumentoRepository documentoRepository;
    private final AdminFeignClient adminFeignClient;

    private long currentLine = 0;

    private final Set<String> cufesInBatch = new HashSet<>();
    private final Set<String> nitFacturasInBatch = new HashSet<>();

    public FacturaZipProcessor(Long identificadorCargue,
                               FacturaService facturaService,
                               ErrorCargueService errorCargueService,
                               DocumentoRepository documentoRepository,
                               AdminFeignClient adminFeignClient) {
        this.identificadorCargue = identificadorCargue;
        this.facturaService = facturaService;
        this.errorCargueService = errorCargueService;
        this.documentoRepository = documentoRepository;
        this.adminFeignClient = adminFeignClient;
    }

    @Override
    public Factura process(FacturaZipWrapperDto item) throws Exception {
        currentLine++;

        File xmlFile = item.getArchivoXml();
        if (xmlFile == null || !xmlFile.exists()) {
            return null;
        }

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(false);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        XPath xPath = XPathFactory.newInstance().newXPath();

        String cdataEmbebido = (String) xPath.compile("//Attachment/ExternalReference/Description/text()").evaluate(doc, XPathConstants.STRING);
        Document docFactura = doc;

        if (cdataEmbebido != null && !cdataEmbebido.isBlank()) {
            try {
                DocumentBuilder builderEmbebido = dbFactory.newDocumentBuilder();
                docFactura = builderEmbebido.parse(new InputSource(new StringReader(cdataEmbebido.trim())));
                docFactura.getDocumentElement().normalize();
            } catch (Exception e) {
                LOGGER.warn("⚠️ No se pudo parsear CDATA interno: {}", e.getMessage());
            }
        }

        String nitEmisor = (String) xPath.compile("//AccountingSupplierParty//CompanyID/text()").evaluate(docFactura, XPathConstants.STRING);
        if (nitEmisor == null || nitEmisor.isBlank()) {
            nitEmisor = (String) xPath.compile("//SenderParty//CompanyID/text()").evaluate(doc, XPathConstants.STRING);
        }

        String razonSocial = (String) xPath.compile("//AccountingSupplierParty//RegistrationName/text()").evaluate(docFactura, XPathConstants.STRING);
        if (razonSocial == null || razonSocial.isBlank()) {
            razonSocial = (String) xPath.compile("//SenderParty//RegistrationName/text()").evaluate(doc, XPathConstants.STRING);
        }

        String numeroFactura = (String) xPath.compile("//ParentDocumentID/text()").evaluate(doc, XPathConstants.STRING);
        if (numeroFactura == null || numeroFactura.isBlank()) {
            numeroFactura = (String) xPath.compile("//Invoice/ID/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        String cufe = (String) xPath.compile("//UUID/text()").evaluate(docFactura, XPathConstants.STRING);
        String fechaStr = (String) xPath.compile("//IssueDate/text()").evaluate(docFactura, XPathConstants.STRING);
        String valorStr = (String) xPath.compile("//LegalMonetaryTotal/PayableAmount/text()").evaluate(docFactura, XPathConstants.STRING);

        // =========================================================================
        // ⚡ VALIDACIÓN 1: ETIQUETAS ESTRUCTURALES OBLIGATORIAS EN XML
        // =========================================================================
        if (nitEmisor == null || nitEmisor.isBlank()) {
            registrarError("CAMPO_OBLIGATORIO_VACIO", "NIT_EMISOR", "El XML no contiene la etiqueta del NIT del emisor.", xmlFile.getName());
        }
        if (numeroFactura == null || numeroFactura.isBlank()) {
            registrarError("CAMPO_OBLIGATORIO_VACIO", "NUMERO_FACTURA", "El XML no contiene el número de factura.", xmlFile.getName());
        }
        if (cufe == null || cufe.isBlank()) {
            registrarError("CAMPO_OBLIGATORIO_VACIO", "CUFE", "El XML no contiene el CUFE / UUID.", xmlFile.getName());
        }

        String nitClean = nitEmisor.trim();
        String numFacturaClean = numeroFactura.trim();
        String cufeClean = cufe.trim();
        String llaveNitFactura = nitClean + "_" + numFacturaClean;

        // =========================================================================
        // 🛑 VALIDACIÓN 2: VERIFICACIÓN DE SOPORTES DILIGENCIADOS POR PRESTADOR
        // =========================================================================
        List<Documento> soportesPrestador = documentoRepository.findSoportesPrestadorByNit(nitClean);
        
        Set<Long> tiposCargados = soportesPrestador.stream()
                .filter(d -> d.getTipoId() != null && d.getDeletedAt() == null)
                .map(Documento::getTipoId)
                .collect(Collectors.toSet());

        List<TipoDto> tiposAdministrativos = new ArrayList<>();
        try {
            if (adminFeignClient != null) {
                tiposAdministrativos = adminFeignClient.listarTipos();
            }
        } catch (Exception e) {
            LOGGER.error("⚠️ No se pudo establecer comunicación Feign con admin-service: {}", e.getMessage());
        }

        List<String> faltantes = new ArrayList<>();
        if (tiposAdministrativos != null && !tiposAdministrativos.isEmpty()) {
            for (TipoDto tipo : tiposAdministrativos) {
                if (tipo.getId() != null && tipo.getId() >= 1L && tipo.getId() <= 4L) {
                    if (!tiposCargados.contains(tipo.getId())) {
                        faltantes.add(tipo.getDescripcion() != null ? tipo.getDescripcion() : "Tipo " + tipo.getId());
                    }
                }
            }
        }

        if (!faltantes.isEmpty()) {
            String errorMsg = String.format("El prestador con NIT [%s] no puede radicar la factura [%s]. Soportes empresariales obligatorios pendientes por cargar: %s",
                    nitClean, numFacturaClean, String.join(", ", faltantes));
            registrarError("SOPORTES_PRESTADOR_INCOMPLETOS", "SOPORTES_EMPRESARIALES", errorMsg, nitClean);
        }

        // =========================================================================
        // ⚡ VALIDACIÓN 3: DUPLICIDAD DENTRO DEL LOTE Y BASE DE DATOS
        // =========================================================================
        if (cufesInBatch.contains(cufeClean)) {
            registrarError("CUFE_DUPLICADO_ZIP", "CUFE", String.format("El CUFE [%s] está duplicado dentro del mismo archivo ZIP.", cufeClean), cufeClean);
        }
        if (nitFacturasInBatch.contains(llaveNitFactura)) {
            registrarError("FACTURA_DUPLICADA_ZIP", "NIT_NUMERO_FACTURA", String.format("La factura [%s] del NIT [%s] está duplicada en el mismo ZIP.", numFacturaClean, nitClean), llaveNitFactura);
        }

        List<String> cufesExistentesBD = facturaService.findExistingCufes(List.of(cufeClean));
        if (!cufesExistentesBD.isEmpty()) {
            registrarError("CUFE_EXISTENTE_BD", "CUFE", String.format("El CUFE [%s] ya existe en la base de datos.", cufeClean), cufeClean);
        }

        List<String> nitFacturasExistentesBD = facturaService.findExistingNitFacturas(List.of(llaveNitFactura));
        if (!nitFacturasExistentesBD.isEmpty()) {
            registrarError("FACTURA_EXISTENTE_BD", "NIT_NUMERO_FACTURA", String.format("La factura [%s] del NIT [%s] ya existe en la base de datos.", numFacturaClean, nitClean), llaveNitFactura);
        }

        cufesInBatch.add(cufeClean);
        nitFacturasInBatch.add(llaveNitFactura);

        // Mapeo de la entidad Factura
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

        // Documento XML de la Factura (Tipo 6 - FAC_XML)
        Documento docXml = new Documento();
        docXml.setNombreOriginal(xmlFile.getName());
        docXml.setTamano(xmlFile.length());
        docXml.setEstadoId(1L);
        docXml.setExtensionId(1L);
        docXml.setTipoId(6L);
        docXml.setArchivoTemporal(xmlFile);
        factura.addDocumento(docXml);

        // Documento PDF de la Factura (Tipo 5 - FAC_PDF)
        if (item.getArchivoPdf() != null && item.getArchivoPdf().exists()) {
            Documento docPdf = new Documento();
            docPdf.setNombreOriginal(item.getArchivoPdf().getName());
            docPdf.setTamano(item.getArchivoPdf().length());
            docPdf.setEstadoId(1L);
            docPdf.setExtensionId(2L);
            docPdf.setTipoId(5L);
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

        errorCargueService.saveAll(List.of(error));
        LOGGER.error("❌ ERROR DETECTADO EN BATCH: {}", descripcion);
        throw new IllegalStateException(descripcion);
    }
}