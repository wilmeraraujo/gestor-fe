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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.gestor_fe.core.client.AdminFeignClient;
import com.gestor_fe.core.dto.FacturaZipWrapperDto;
import com.gestor_fe.core.dto.TipoDto;
import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.ErrorCargue;
import com.gestor_fe.core.entity.Factura;
import com.gestor_fe.core.entity.FacturaItem;
import com.gestor_fe.core.repository.DocumentoRepository;
import com.gestor_fe.core.service.ErrorCargueService;
import com.gestor_fe.core.service.FacturaService;

public class FacturaZipProcessor implements ItemProcessor<FacturaZipWrapperDto, Factura> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacturaZipProcessor.class);

    private final Long identificadorCargue;
    private final String usuarioAutenticado;
    private final boolean esAdmin;
    private final String codigoMovimiento;
    private final FacturaService facturaService;
    private final ErrorCargueService errorCargueService;
    private final DocumentoRepository documentoRepository;
    private final AdminFeignClient adminFeignClient;

    private long currentLine = 0;

    private final Set<String> cufesInBatch = new HashSet<>();
    private final Set<String> nitFacturasInBatch = new HashSet<>();

    // ⚡ CACHÉ EN MEMORIA (Ámbito de ejecución del Job): Evita llamadas redundantes a Feign y Base de Datos por cada factura
    private List<TipoDto> tiposAdministrativosCache = null;
    private final java.util.Map<String, Set<String>> soportesCargadosPorNitCache = new java.util.HashMap<>();

    public FacturaZipProcessor(Long identificadorCargue,
                               String usuarioAutenticado,
                               FacturaService facturaService,
                               ErrorCargueService errorCargueService,
                               DocumentoRepository documentoRepository,
                               AdminFeignClient adminFeignClient) {
        this(identificadorCargue, usuarioAutenticado, false, null, facturaService, errorCargueService, documentoRepository, adminFeignClient);
    }

    public FacturaZipProcessor(Long identificadorCargue,
                               String usuarioAutenticado,
                               boolean esAdmin,
                               FacturaService facturaService,
                               ErrorCargueService errorCargueService,
                               DocumentoRepository documentoRepository,
                               AdminFeignClient adminFeignClient) {
        this(identificadorCargue, usuarioAutenticado, esAdmin, null, facturaService, errorCargueService, documentoRepository, adminFeignClient);
    }

    public FacturaZipProcessor(Long identificadorCargue,
                               String usuarioAutenticado,
                               boolean esAdmin,
                               String codigoMovimiento,
                               FacturaService facturaService,
                               ErrorCargueService errorCargueService,
                               DocumentoRepository documentoRepository,
                               AdminFeignClient adminFeignClient) {
        this.identificadorCargue = identificadorCargue;
        this.usuarioAutenticado = usuarioAutenticado;
        this.esAdmin = esAdmin;
        this.codigoMovimiento = codigoMovimiento;
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

        String dv = (String) xPath.compile("//AccountingSupplierParty//CompanyID/@schemeID").evaluate(docFactura, XPathConstants.STRING);
        if (dv == null || dv.isBlank()) {
            dv = (String) xPath.compile("//SenderParty//CompanyID/@schemeID").evaluate(doc, XPathConstants.STRING);
        }

        String razonSocial = (String) xPath.compile("//AccountingSupplierParty//RegistrationName/text()").evaluate(docFactura, XPathConstants.STRING);
        if (razonSocial == null || razonSocial.isBlank()) {
            razonSocial = (String) xPath.compile("//SenderParty//RegistrationName/text()").evaluate(doc, XPathConstants.STRING);
        }

        String tipoIdentificacionEmisor = (String) xPath.compile("//AccountingSupplierParty//CompanyID/@schemeName").evaluate(docFactura, XPathConstants.STRING);
        if (tipoIdentificacionEmisor == null || tipoIdentificacionEmisor.isBlank()) {
            tipoIdentificacionEmisor = (String) xPath.compile("//AccountingSupplierParty//CompanyID/@schemeAgencyID").evaluate(docFactura, XPathConstants.STRING);
        }
        if (tipoIdentificacionEmisor == null || tipoIdentificacionEmisor.isBlank()) {
            tipoIdentificacionEmisor = (String) xPath.compile("//SenderParty//CompanyID/@schemeName").evaluate(doc, XPathConstants.STRING);
        }

        String tipoPersonaEmisor = (String) xPath.compile("//AccountingSupplierParty//AdditionalAccountID/text()").evaluate(docFactura, XPathConstants.STRING);
        if (tipoPersonaEmisor == null || tipoPersonaEmisor.isBlank()) {
            tipoPersonaEmisor = (String) xPath.compile("//SenderParty//AdditionalAccountID/text()").evaluate(doc, XPathConstants.STRING);
        }

        String respFiscalEmisor = (String) xPath.compile("//AccountingSupplierParty//PartyTaxScheme/TaxLevelCode/text()").evaluate(docFactura, XPathConstants.STRING);
        if (respFiscalEmisor == null || respFiscalEmisor.isBlank()) {
            respFiscalEmisor = (String) xPath.compile("//AccountingSupplierParty//TaxLevelCode/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        // Extracción de datos de Persona Natural (si aplica en el XML)
        String primerNombre = (String) xPath.compile("//AccountingSupplierParty//Person/FirstName/text()").evaluate(docFactura, XPathConstants.STRING);
        String segundoNombre = (String) xPath.compile("//AccountingSupplierParty//Person/MiddleName/text()").evaluate(docFactura, XPathConstants.STRING);
        String primerApellido = (String) xPath.compile("//AccountingSupplierParty//Person/FamilyName/text()").evaluate(docFactura, XPathConstants.STRING);
        String segundoApellido = (String) xPath.compile("//AccountingSupplierParty//Person/SecondFamilyName/text()").evaluate(docFactura, XPathConstants.STRING);

        if ((razonSocial == null || razonSocial.isBlank()) && primerNombre != null && !primerNombre.isBlank()) {
            StringBuilder sb = new StringBuilder();
            sb.append(primerNombre.trim());
            if (segundoNombre != null && !segundoNombre.isBlank()) sb.append(" ").append(segundoNombre.trim());
            if (primerApellido != null && !primerApellido.isBlank()) sb.append(" ").append(primerApellido.trim());
            if (segundoApellido != null && !segundoApellido.isBlank()) sb.append(" ").append(segundoApellido.trim());
            razonSocial = sb.toString();
        }

        // Extracción de Ubicación y Dirección
        String direccion = (String) xPath.compile("//AccountingSupplierParty//PhysicalLocation//AddressLine/Line/text()").evaluate(docFactura, XPathConstants.STRING);
        if (direccion == null || direccion.isBlank()) {
            direccion = (String) xPath.compile("//AccountingSupplierParty//RegistrationAddress//AddressLine/Line/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        String codigoMunicipio = (String) xPath.compile("//AccountingSupplierParty//PhysicalLocation//Address/ID/text()").evaluate(docFactura, XPathConstants.STRING);
        if (codigoMunicipio == null || codigoMunicipio.isBlank()) {
            codigoMunicipio = (String) xPath.compile("//AccountingSupplierParty//RegistrationAddress/ID/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        String codigoDepartamento = (String) xPath.compile("//AccountingSupplierParty//PhysicalLocation//Address/CountrySubentityCode/text()").evaluate(docFactura, XPathConstants.STRING);
        if (codigoDepartamento == null || codigoDepartamento.isBlank()) {
            codigoDepartamento = (String) xPath.compile("//AccountingSupplierParty//RegistrationAddress/CountrySubentityCode/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        String codigoPais = (String) xPath.compile("//AccountingSupplierParty//PhysicalLocation//Address/Country/IdentificationCode/text()").evaluate(docFactura, XPathConstants.STRING);
        if (codigoPais == null || codigoPais.isBlank()) {
            codigoPais = (String) xPath.compile("//AccountingSupplierParty//RegistrationAddress/Country/IdentificationCode/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        String numeroFactura = (String) xPath.compile("//ParentDocumentID/text()").evaluate(doc, XPathConstants.STRING);
        if (numeroFactura == null || numeroFactura.isBlank()) {
            numeroFactura = (String) xPath.compile("//Invoice/ID/text()").evaluate(docFactura, XPathConstants.STRING);
        }
        if (numeroFactura == null || numeroFactura.isBlank()) {
            numeroFactura = (String) xPath.compile("//CreditNote/ID/text()").evaluate(docFactura, XPathConstants.STRING);
        }
        if (numeroFactura == null || numeroFactura.isBlank()) {
            numeroFactura = (String) xPath.compile("//DebitNote/ID/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        String cufe = (String) xPath.compile("//UUID/text()").evaluate(docFactura, XPathConstants.STRING);
        String fechaStr = (String) xPath.compile("//IssueDate/text()").evaluate(docFactura, XPathConstants.STRING);
        String horaStr = (String) xPath.compile("//IssueTime/text()").evaluate(docFactura, XPathConstants.STRING);

        // Tipo de documento y operación
        String tipoDoc = (String) xPath.compile("//InvoiceTypeCode/text()").evaluate(docFactura, XPathConstants.STRING);
        if (tipoDoc == null || tipoDoc.isBlank()) {
            tipoDoc = (String) xPath.compile("//CreditNoteTypeCode/text()").evaluate(docFactura, XPathConstants.STRING);
        }
        if (tipoDoc == null || tipoDoc.isBlank()) {
            String rootName = docFactura.getDocumentElement() != null ? docFactura.getDocumentElement().getLocalName() : "";
            if ("CreditNote".equalsIgnoreCase(rootName)) tipoDoc = "91";
            else if ("DebitNote".equalsIgnoreCase(rootName)) tipoDoc = "92";
            else tipoDoc = "01";
        }

        String tipoOp = (String) xPath.compile("//CustomizationID/text()").evaluate(docFactura, XPathConstants.STRING);
        String ambEjec = (String) xPath.compile("//ProfileExecutionID/text()").evaluate(docFactura, XPathConstants.STRING);
        String monedaStr = (String) xPath.compile("//DocumentCurrencyCode/text()").evaluate(docFactura, XPathConstants.STRING);
        if (monedaStr == null || monedaStr.isBlank()) {
            monedaStr = (String) xPath.compile("//Invoice/@DocumentCurrencyCode").evaluate(docFactura, XPathConstants.STRING);
        }

        String tasaCambioStr = (String) xPath.compile("//PaymentExchangeRate/CalculationRate/text()").evaluate(docFactura, XPathConstants.STRING);
        String fechaTasaCambioStr = (String) xPath.compile("//PaymentExchangeRate/Date/text()").evaluate(docFactura, XPathConstants.STRING);

        String valorStr = (String) xPath.compile("//LegalMonetaryTotal/PayableAmount/text()").evaluate(docFactura, XPathConstants.STRING);
        String valorBrutoStr = (String) xPath.compile("//LegalMonetaryTotal/LineExtensionAmount/text()").evaluate(docFactura, XPathConstants.STRING);
        String totalDescStr = (String) xPath.compile("//LegalMonetaryTotal/AllowanceTotalAmount/text()").evaluate(docFactura, XPathConstants.STRING);
        String totalCargosStr = (String) xPath.compile("//LegalMonetaryTotal/ChargeTotalAmount/text()").evaluate(docFactura, XPathConstants.STRING);
        String totalAnticiposStr = (String) xPath.compile("//LegalMonetaryTotal/PrepaidAmount/text()").evaluate(docFactura, XPathConstants.STRING);

        // Extracción de Subtotal e Impuesto IVA
        String subtotalStr = (String) xPath.compile("//LegalMonetaryTotal/TaxExclusiveAmount/text()").evaluate(docFactura, XPathConstants.STRING);
        if (subtotalStr == null || subtotalStr.isBlank()) {
            subtotalStr = valorBrutoStr;
        }

        String ivaStr = (String) xPath.compile("//TaxTotal[TaxSubtotal/TaxCategory/TaxScheme/ID='01']/TaxAmount/text()").evaluate(docFactura, XPathConstants.STRING);
        if (ivaStr == null || ivaStr.isBlank()) {
            ivaStr = (String) xPath.compile("//TaxSubtotal[TaxCategory/TaxScheme/ID='01' or TaxCategory/TaxScheme/Name='IVA']/TaxAmount/text()").evaluate(docFactura, XPathConstants.STRING);
        }
        if (ivaStr == null || ivaStr.isBlank()) {
            ivaStr = (String) xPath.compile("//TaxTotal/TaxAmount/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        // Impoconsumo (INC - Código 04)
        String impoconsumoStr = (String) xPath.compile("//TaxTotal[TaxSubtotal/TaxCategory/TaxScheme/ID='04' or TaxSubtotal/TaxCategory/TaxScheme/Name='INC']/TaxAmount/text()").evaluate(docFactura, XPathConstants.STRING);

        // =========================================================================
        // 👤 EXTRACCIÓN DE DATOS DEL CLIENTE / ADQUIRENTE (AccountingCustomerParty)
        // =========================================================================
        String tipoIdCli = (String) xPath.compile("//AccountingCustomerParty//CompanyID/@schemeName").evaluate(docFactura, XPathConstants.STRING);
        if (tipoIdCli == null || tipoIdCli.isBlank()) {
            tipoIdCli = (String) xPath.compile("//AccountingCustomerParty//CompanyID/@schemeAgencyID").evaluate(docFactura, XPathConstants.STRING);
        }
        if (tipoIdCli == null || tipoIdCli.isBlank()) {
            tipoIdCli = (String) xPath.compile("//ReceiverParty//CompanyID/@schemeName").evaluate(doc, XPathConstants.STRING);
        }

        String tipoPersCli = (String) xPath.compile("//AccountingCustomerParty//AdditionalAccountID/text()").evaluate(docFactura, XPathConstants.STRING);
        if (tipoPersCli == null || tipoPersCli.isBlank()) {
            tipoPersCli = (String) xPath.compile("//ReceiverParty//AdditionalAccountID/text()").evaluate(doc, XPathConstants.STRING);
        }

        String respFiscalCli = (String) xPath.compile("//AccountingCustomerParty//PartyTaxScheme/TaxLevelCode/text()").evaluate(docFactura, XPathConstants.STRING);
        if (respFiscalCli == null || respFiscalCli.isBlank()) {
            respFiscalCli = (String) xPath.compile("//AccountingCustomerParty//TaxLevelCode/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        String nitCliente = (String) xPath.compile("//AccountingCustomerParty//CompanyID/text()").evaluate(docFactura, XPathConstants.STRING);
        if (nitCliente == null || nitCliente.isBlank()) {
            nitCliente = (String) xPath.compile("//ReceiverParty//CompanyID/text()").evaluate(doc, XPathConstants.STRING);
        }

        String dvCliente = (String) xPath.compile("//AccountingCustomerParty//CompanyID/@schemeID").evaluate(docFactura, XPathConstants.STRING);
        if (dvCliente == null || dvCliente.isBlank()) {
            dvCliente = (String) xPath.compile("//ReceiverParty//CompanyID/@schemeID").evaluate(doc, XPathConstants.STRING);
        }

        String razonSocialCliente = (String) xPath.compile("//AccountingCustomerParty//PartyTaxScheme/RegistrationName/text()").evaluate(docFactura, XPathConstants.STRING);
        if (razonSocialCliente == null || razonSocialCliente.isBlank()) {
            razonSocialCliente = (String) xPath.compile("//AccountingCustomerParty//PartyLegalEntity/RegistrationName/text()").evaluate(docFactura, XPathConstants.STRING);
        }
        if (razonSocialCliente == null || razonSocialCliente.isBlank()) {
            razonSocialCliente = (String) xPath.compile("//AccountingCustomerParty//PartyName/Name/text()").evaluate(docFactura, XPathConstants.STRING);
        }
        if (razonSocialCliente == null || razonSocialCliente.isBlank()) {
            razonSocialCliente = (String) xPath.compile("//ReceiverParty//RegistrationName/text()").evaluate(doc, XPathConstants.STRING);
        }
        if (razonSocialCliente == null || razonSocialCliente.isBlank()) {
            String pNomCli = (String) xPath.compile("//AccountingCustomerParty//Person/FirstName/text()").evaluate(docFactura, XPathConstants.STRING);
            String sNomCli = (String) xPath.compile("//AccountingCustomerParty//Person/MiddleName/text()").evaluate(docFactura, XPathConstants.STRING);
            String pApeCli = (String) xPath.compile("//AccountingCustomerParty//Person/FamilyName/text()").evaluate(docFactura, XPathConstants.STRING);
            String sApeCli = (String) xPath.compile("//AccountingCustomerParty//Person/SecondFamilyName/text()").evaluate(docFactura, XPathConstants.STRING);
            if (pNomCli != null && !pNomCli.isBlank()) {
                StringBuilder sbCli = new StringBuilder(pNomCli.trim());
                if (sNomCli != null && !sNomCli.isBlank()) sbCli.append(" ").append(sNomCli.trim());
                if (pApeCli != null && !pApeCli.isBlank()) sbCli.append(" ").append(pApeCli.trim());
                if (sApeCli != null && !sApeCli.isBlank()) sbCli.append(" ").append(sApeCli.trim());
                razonSocialCliente = sbCli.toString();
            }
        }

        String direccionCliente = (String) xPath.compile("//AccountingCustomerParty//PhysicalLocation//AddressLine/Line/text()").evaluate(docFactura, XPathConstants.STRING);
        if (direccionCliente == null || direccionCliente.isBlank()) {
            direccionCliente = (String) xPath.compile("//AccountingCustomerParty//RegistrationAddress//AddressLine/Line/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        String ciudadCliente = (String) xPath.compile("//AccountingCustomerParty//PhysicalLocation//Address/CityName/text()").evaluate(docFactura, XPathConstants.STRING);
        if (ciudadCliente == null || ciudadCliente.isBlank()) {
            ciudadCliente = (String) xPath.compile("//AccountingCustomerParty//RegistrationAddress/CityName/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        String deptoCliente = (String) xPath.compile("//AccountingCustomerParty//PhysicalLocation//Address/CountrySubentity/text()").evaluate(docFactura, XPathConstants.STRING);
        if (deptoCliente == null || deptoCliente.isBlank()) {
            deptoCliente = (String) xPath.compile("//AccountingCustomerParty//RegistrationAddress/CountrySubentity/text()").evaluate(docFactura, XPathConstants.STRING);
        }

        String telCliente = (String) xPath.compile("//AccountingCustomerParty//Contact/Telephone/text()").evaluate(docFactura, XPathConstants.STRING);
        String emailCliente = (String) xPath.compile("//AccountingCustomerParty//Contact/ElectronicMail/text()").evaluate(docFactura, XPathConstants.STRING);

        // =========================================================================
        // 💰 CONDICIONES COMERCIALES Y RETENCIONES
        // =========================================================================
        String fechaVencimientoStr = (String) xPath.compile("//DueDate/text()").evaluate(docFactura, XPathConstants.STRING);
        String formaPago = (String) xPath.compile("//PaymentMeans/ID/text()").evaluate(docFactura, XPathConstants.STRING);
        String medioPago = (String) xPath.compile("//PaymentMeans/PaymentMeansCode/text()").evaluate(docFactura, XPathConstants.STRING);

        NodeList notasNodes = (NodeList) xPath.compile("//Note[normalize-space(text()) != '']").evaluate(docFactura, XPathConstants.NODESET);
        StringBuilder notasSb = new StringBuilder();
        if (notasNodes != null) {
            for (int i = 0; i < notasNodes.getLength(); i++) {
                String nTxt = notasNodes.item(i).getTextContent();
                if (nTxt != null && !nTxt.trim().isEmpty()) {
                    if (notasSb.length() > 0) notasSb.append(" | ");
                    notasSb.append(nTxt.trim());
                }
            }
        }
        String notas = notasSb.length() > 0 ? notasSb.toString() : null;

        String reteFuenteStr = (String) xPath.compile("//WithholdingTaxTotal[TaxSubtotal/TaxCategory/TaxScheme/ID='06' or TaxSubtotal/TaxCategory/TaxScheme/Name='ReteRenta' or TaxSubtotal/TaxCategory/TaxScheme/Name='ReteFuente']/TaxAmount/text()").evaluate(docFactura, XPathConstants.STRING);
        String reteIcaStr = (String) xPath.compile("//WithholdingTaxTotal[TaxSubtotal/TaxCategory/TaxScheme/ID='07' or TaxSubtotal/TaxCategory/TaxScheme/Name='ReteICA']/TaxAmount/text()").evaluate(docFactura, XPathConstants.STRING);
        String reteIvaStr = (String) xPath.compile("//WithholdingTaxTotal[TaxSubtotal/TaxCategory/TaxScheme/ID='05' or TaxSubtotal/TaxCategory/TaxScheme/Name='ReteIVA']/TaxAmount/text()").evaluate(docFactura, XPathConstants.STRING);

        NodeList retNodes = (NodeList) xPath.compile("//WithholdingTaxTotal/TaxAmount/text()").evaluate(docFactura, XPathConstants.NODESET);
        BigDecimal totalRetencionesCalc = BigDecimal.ZERO;
        if (retNodes != null) {
            for (int i = 0; i < retNodes.getLength(); i++) {
                try {
                    String v = retNodes.item(i).getTextContent();
                    if (v != null && !v.isBlank()) {
                        totalRetencionesCalc = totalRetencionesCalc.add(new BigDecimal(v.trim()));
                    }
                } catch (Exception ignored) {}
            }
        }

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

        // Si es Administrador (admin o gestor-fe-admin), el NIT de referencia para validar soportes es el NIT del XML (nitClean)
        // Si es Prestador (rol gestor-fe-prestador), el NIT de referencia es su usuario autenticado (que es su NIT)
        String nitReferencia = esAdmin ? nitClean : (usuarioAutenticado != null && !usuarioAutenticado.isBlank() ? usuarioAutenticado.trim() : nitClean);

        // =========================================================================
        // 🛑 VALIDACIÓN 1.1: CORRESPONDENCIA DE NIT CON EL USUARIO AUTENTICADO
        // Si NO es admin (es decir, es rol gestor-fe-prestador / usuario prestador),
        // se valida estrictamente que el NIT del XML coincida con el usuario autenticado (NIT del prestador).
        // Si es admin o gestor-fe-admin, se omite esta validación permitiendo cargar cualquier prestador.
        // =========================================================================
        if (!esAdmin && usuarioAutenticado != null && !usuarioAutenticado.isBlank()) {
            if (!nitClean.equalsIgnoreCase(usuarioAutenticado.trim())) {
                String errorUsuario = String.format("El NIT emisor de la factura [%s] en el XML no coincide con el NIT del usuario autenticado [%s] para la factura [%s].",
                        nitClean, usuarioAutenticado.trim(), numFacturaClean);
                registrarError("NIT_NO_CORRESPONDE_USUARIO", "NIT_EMISOR", errorUsuario, nitClean);
            }
        }

        // =========================================================================
        // 🛑 VALIDACIÓN 2: VERIFICACIÓN DE SOPORTES DILIGENCIADOS POR PRESTADOR
        // =========================================================================
        // ⚡ Optimización: Consultar la BD solo una vez por NIT de referencia durante todo el lote
        Set<String> tiposCargados = soportesCargadosPorNitCache.computeIfAbsent(nitReferencia, nit -> {
            List<Documento> soportes = documentoRepository.findSoportesPrestadorByNit(nit);
            return soportes.stream()
                    .filter(d -> d.getCodigoTipo() != null && d.getDeletedAt() == null)
                    .map(Documento::getCodigoTipo)
                    .collect(Collectors.toSet());
        });

        // ⚡ Optimización: Consultar Feign a admin-service una sola vez durante el Job
        if (this.tiposAdministrativosCache == null) {
            try {
                if (adminFeignClient != null) {
                    this.tiposAdministrativosCache = adminFeignClient.listarTipos();
                }
            } catch (Exception e) {
                LOGGER.error("⚠️ No se pudo establecer comunicación Feign con admin-service: {}", e.getMessage());
                this.tiposAdministrativosCache = new ArrayList<>();
            }
        }
        List<TipoDto> tiposAdministrativos = this.tiposAdministrativosCache;

        List<String> faltantes = new ArrayList<>();
        if (tiposAdministrativos != null && !tiposAdministrativos.isEmpty()) {
            for (TipoDto tipo : tiposAdministrativos) {
                if (tipo.getId() != null && tipo.getId() >= 1L && tipo.getId() <= 4L) {
                    String codTipo = tipo.getCodigo() != null && !tipo.getCodigo().isBlank() 
                            ? tipo.getCodigo().trim() 
                            : String.valueOf(tipo.getId());
                    if (!tiposCargados.contains(codTipo) && !tiposCargados.contains(String.valueOf(tipo.getId()))) {
                        faltantes.add(tipo.getDescripcion() != null ? tipo.getDescripcion() : "Tipo " + tipo.getId());
                    }
                }
            }
        }

        if (!faltantes.isEmpty()) {
            String errorMsg = String.format("El prestador con NIT [%s] no puede radicar la factura [%s]. Soportes empresariales obligatorios pendientes por cargar: %s",
                    nitReferencia, numFacturaClean, String.join(", ", faltantes));
            registrarError("SOPORTES_PRESTADOR_INCOMPLETOS", "SOPORTES_EMPRESARIALES", errorMsg, nitReferencia);
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
        if (dv != null && !dv.isBlank()) {
            String dvTrim = dv.trim();
            factura.setDv(dvTrim.length() > 2 ? dvTrim.substring(0, 2) : dvTrim);
        }
        factura.setPrimerNombre(primerNombre != null && !primerNombre.isBlank() ? primerNombre.trim() : null);
        factura.setSegundoNombre(segundoNombre != null && !segundoNombre.isBlank() ? segundoNombre.trim() : null);
        factura.setPrimerApellido(primerApellido != null && !primerApellido.isBlank() ? primerApellido.trim() : null);
        factura.setSegundoApellido(segundoApellido != null && !segundoApellido.isBlank() ? segundoApellido.trim() : null);

        factura.setDireccion(direccion != null && !direccion.isBlank() ? direccion.trim() : null);
        factura.setCodigoDepartamento(codigoDepartamento != null && !codigoDepartamento.isBlank() ? codigoDepartamento.trim() : null);
        factura.setCodigoMunicipio(codigoMunicipio != null && !codigoMunicipio.isBlank() ? codigoMunicipio.trim() : null);
        factura.setCodigoPais(codigoPais != null && !codigoPais.isBlank() ? codigoPais.trim() : null);

        factura.setCodigoTipoIdentificacionEmisor(tipoIdentificacionEmisor != null && !tipoIdentificacionEmisor.isBlank() ? tipoIdentificacionEmisor.trim() : null);
        factura.setCodigoTipoPersonaEmisor(tipoPersonaEmisor != null && !tipoPersonaEmisor.isBlank() ? tipoPersonaEmisor.trim() : null);
        factura.setCodigoResponsabilidadFiscalEmisor(respFiscalEmisor != null && !respFiscalEmisor.isBlank() ? respFiscalEmisor.trim() : null);

        factura.setRazonSocialEmisor(razonSocial != null ? razonSocial.trim() : "DESCONOCIDO");
        factura.setNumeroFactura(numFacturaClean);
        factura.setCufe(cufeClean);
        factura.setIdentificadorCargue(identificadorCargue);
        factura.setCodigoMovimiento(this.codigoMovimiento);
        factura.setLinea(currentLine);

        // Metadatos DIAN y Moneda
        factura.setCodigoTipoDocumento(tipoDoc != null && !tipoDoc.isBlank() ? tipoDoc.trim() : "01");
        factura.setCodigoTipoOperacion(tipoOp != null && !tipoOp.isBlank() ? tipoOp.trim() : null);
        factura.setCodigoAmbienteEjecucion(ambEjec != null && !ambEjec.isBlank() ? ambEjec.trim() : null);
        factura.setMoneda(monedaStr != null && !monedaStr.isBlank() ? monedaStr.trim() : "COP");
        factura.setHoraEmision(horaStr != null && !horaStr.isBlank() ? horaStr.trim() : null);

        if (tasaCambioStr != null && !tasaCambioStr.isBlank()) {
            try { factura.setTasaCambio(new BigDecimal(tasaCambioStr.trim())); } catch (Exception ignored) {}
        }
        if (fechaTasaCambioStr != null && !fechaTasaCambioStr.isBlank()) {
            try { factura.setFechaTasaCambio(LocalDate.parse(fechaTasaCambioStr.trim())); } catch (Exception ignored) {}
        }

        if (fechaStr != null && !fechaStr.isBlank()) {
            factura.setFechaEmision(LocalDate.parse(fechaStr.trim()));
        }
        if (valorBrutoStr != null && !valorBrutoStr.isBlank()) {
            try { factura.setValorBruto(new BigDecimal(valorBrutoStr.trim())); } catch (Exception ignored) {}
        }
        if (subtotalStr != null && !subtotalStr.isBlank()) {
            try {
                factura.setValorSubtotal(new BigDecimal(subtotalStr.trim()));
            } catch (Exception e) {
                LOGGER.warn("⚠️ No se pudo parsear el subtotal: {}", subtotalStr);
            }
        }
        if (totalDescStr != null && !totalDescStr.isBlank()) {
            try { factura.setTotalDescuentos(new BigDecimal(totalDescStr.trim())); } catch (Exception ignored) {}
        }
        if (totalCargosStr != null && !totalCargosStr.isBlank()) {
            try { factura.setTotalCargos(new BigDecimal(totalCargosStr.trim())); } catch (Exception ignored) {}
        }
        if (totalAnticiposStr != null && !totalAnticiposStr.isBlank()) {
            try { factura.setTotalAnticipos(new BigDecimal(totalAnticiposStr.trim())); } catch (Exception ignored) {}
        }

        if (ivaStr != null && !ivaStr.isBlank()) {
            try {
                factura.setValorIva(new BigDecimal(ivaStr.trim()));
            } catch (Exception e) {
                LOGGER.warn("⚠️ No se pudo parsear el valor IVA: {}", ivaStr);
            }
        } else {
            factura.setValorIva(BigDecimal.ZERO);
        }

        if (impoconsumoStr != null && !impoconsumoStr.isBlank()) {
            try { factura.setValorImpoconsumo(new BigDecimal(impoconsumoStr.trim())); } catch (Exception ignored) {}
        }

        if (valorStr != null && !valorStr.isBlank()) {
            factura.setValorTotal(new BigDecimal(valorStr.trim()));
        }

        // 👤 Datos del Cliente
        factura.setCodigoTipoIdentificacionCliente(tipoIdCli != null && !tipoIdCli.isBlank() ? tipoIdCli.trim() : null);
        factura.setCodigoTipoPersonaCliente(tipoPersCli != null && !tipoPersCli.isBlank() ? tipoPersCli.trim() : null);
        factura.setCodigoResponsabilidadFiscalCliente(respFiscalCli != null && !respFiscalCli.isBlank() ? respFiscalCli.trim() : null);
        factura.setNitCliente(nitCliente != null && !nitCliente.isBlank() ? nitCliente.trim() : null);
        factura.setDvCliente(dvCliente != null && !dvCliente.isBlank() ? dvCliente.trim() : null);
        factura.setRazonSocialCliente(razonSocialCliente != null && !razonSocialCliente.isBlank() ? razonSocialCliente.trim() : null);
        factura.setDireccionCliente(direccionCliente != null && !direccionCliente.isBlank() ? direccionCliente.trim() : null);
        factura.setCiudadCliente(ciudadCliente != null && !ciudadCliente.isBlank() ? ciudadCliente.trim() : null);
        factura.setDepartamentoCliente(deptoCliente != null && !deptoCliente.isBlank() ? deptoCliente.trim() : null);
        factura.setTelefonoCliente(telCliente != null && !telCliente.isBlank() ? telCliente.trim() : null);
        factura.setEmailCliente(emailCliente != null && !emailCliente.isBlank() ? emailCliente.trim() : null);

        // 💰 Condiciones Comerciales y Notas
        if (fechaVencimientoStr != null && !fechaVencimientoStr.isBlank()) {
            try {
                factura.setFechaVencimiento(LocalDate.parse(fechaVencimientoStr.trim()));
            } catch (Exception ignored) {}
        }
        factura.setCodigoFormaPago(formaPago != null && !formaPago.isBlank() ? formaPago.trim() : null);
        factura.setCodigoMedioPago(medioPago != null && !medioPago.isBlank() ? medioPago.trim() : null);
        factura.setNotas(notas);

        // 📊 Retenciones
        if (reteFuenteStr != null && !reteFuenteStr.isBlank()) {
            try { factura.setValorRetefuente(new BigDecimal(reteFuenteStr.trim())); } catch (Exception ignored) {}
        }
        if (reteIcaStr != null && !reteIcaStr.isBlank()) {
            try { factura.setValorReteica(new BigDecimal(reteIcaStr.trim())); } catch (Exception ignored) {}
        }
        if (reteIvaStr != null && !reteIvaStr.isBlank()) {
            try { factura.setValorReteiva(new BigDecimal(reteIvaStr.trim())); } catch (Exception ignored) {}
        }
        if (totalRetencionesCalc.compareTo(BigDecimal.ZERO) > 0) {
            factura.setTotalRetenciones(totalRetencionesCalc);
        }

        // 📦 Extracción de Ítems / Líneas Facturadas (InvoiceLine con soporte universal de prefijos)
        try {
            NodeList lineNodes = (NodeList) xPath.compile("//*[local-name()='InvoiceLine' or local-name()='CreditNoteLine' or local-name()='DebitNoteLine']").evaluate(docFactura, XPathConstants.NODESET);
            if (lineNodes != null && lineNodes.getLength() > 0) {
                for (int i = 0; i < lineNodes.getLength(); i++) {
                    Element lineElem = (Element) lineNodes.item(i);
                    FacturaItem itemFactura = new FacturaItem();

                    String numLineaStr = (String) xPath.compile(".//*[local-name()='ID']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (numLineaStr != null && !numLineaStr.isBlank()) {
                        try {
                            itemFactura.setNumeroLinea(Integer.parseInt(numLineaStr.trim()));
                        } catch (Exception e) {
                            itemFactura.setNumeroLinea(i + 1);
                        }
                    } else {
                        itemFactura.setNumeroLinea(i + 1);
                    }

                    String codProd = (String) xPath.compile(".//*[local-name()='SellersItemIdentification']/*[local-name()='ID']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (codProd == null || codProd.isBlank()) {
                        codProd = (String) xPath.compile(".//*[local-name()='StandardItemIdentification']/*[local-name()='ID']/text()").evaluate(lineElem, XPathConstants.STRING);
                    }
                    if (codProd == null || codProd.isBlank()) {
                        codProd = (String) xPath.compile(".//*[local-name()='Item']/*[local-name()='SellersItemIdentification']/*[local-name()='ID']/text()").evaluate(lineElem, XPathConstants.STRING);
                    }
                    itemFactura.setCodigoProducto(codProd != null && !codProd.isBlank() ? codProd.trim() : null);

                    // Código UNSPSC / Estándar
                    String unspsc = (String) xPath.compile(".//*[local-name()='StandardItemIdentification']/*[local-name()='ID'][@schemeID='001' or @schemeAgencyID='10' or @schemeName='UNSPSC']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (unspsc == null || unspsc.isBlank()) {
                        unspsc = (String) xPath.compile(".//*[local-name()='ItemClassificationCode']/text()").evaluate(lineElem, XPathConstants.STRING);
                    }
                    itemFactura.setCodigoUnspsc(unspsc != null && !unspsc.isBlank() ? unspsc.trim() : null);

                    String desc = (String) xPath.compile(".//*[local-name()='Item']/*[local-name()='Description']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (desc == null || desc.isBlank()) {
                        desc = (String) xPath.compile(".//*[local-name()='Description']/text()").evaluate(lineElem, XPathConstants.STRING);
                    }
                    itemFactura.setDescripcion(desc != null && !desc.isBlank() ? desc.trim() : null);

                    String qtyStr = (String) xPath.compile(".//*[local-name()='InvoicedQuantity' or local-name()='CreditedQuantity' or local-name()='DebitedQuantity']/text()").evaluate(lineElem, XPathConstants.STRING);
                    String unitCode = (String) xPath.compile(".//*[local-name()='InvoicedQuantity' or local-name()='CreditedQuantity' or local-name()='DebitedQuantity']/@unitCode").evaluate(lineElem, XPathConstants.STRING);
                    if (qtyStr != null && !qtyStr.isBlank()) {
                        try {
                            itemFactura.setCantidad(new BigDecimal(qtyStr.trim()));
                        } catch (Exception ignored) {}
                    }
                    itemFactura.setUnidadMedida(unitCode != null && !unitCode.isBlank() ? unitCode.trim() : null);

                    String priceStr = (String) xPath.compile(".//*[local-name()='Price']/*[local-name()='PriceAmount']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (priceStr != null && !priceStr.isBlank()) {
                        try {
                            itemFactura.setPrecioUnitario(new BigDecimal(priceStr.trim()));
                        } catch (Exception ignored) {}
                    }

                    String refPriceStr = (String) xPath.compile(".//*[local-name()='PricingReference']//*[local-name()='PriceAmount']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (refPriceStr != null && !refPriceStr.isBlank()) {
                        try {
                            itemFactura.setPrecioReferencia(new BigDecimal(refPriceStr.trim()));
                        } catch (Exception ignored) {}
                    }

                    String descFactorStr = (String) xPath.compile(".//*[local-name()='AllowanceCharge']/*[local-name()='MultiplierFactorNumeric']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (descFactorStr != null && !descFactorStr.isBlank()) {
                        try {
                            itemFactura.setPorcentajeDescuento(new BigDecimal(descFactorStr.trim()));
                        } catch (Exception ignored) {}
                    }

                    String descMontoStr = (String) xPath.compile(".//*[local-name()='AllowanceCharge']/*[local-name()='Amount']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (descMontoStr != null && !descMontoStr.isBlank()) {
                        try {
                            itemFactura.setValorDescuento(new BigDecimal(descMontoStr.trim()));
                        } catch (Exception ignored) {}
                    }

                    // Impuestos por ítem (IVA e Impoconsumo)
                    String itemIvaPct = (String) xPath.compile(".//*[local-name()='TaxSubtotal'][*[local-name()='TaxCategory']/*[local-name()='TaxScheme']/*[local-name()='ID']='01' or *[local-name()='TaxCategory']/*[local-name()='TaxScheme']/*[local-name()='Name']='IVA']/*[local-name()='TaxCategory']/*[local-name()='Percent']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (itemIvaPct == null || itemIvaPct.isBlank()) {
                        itemIvaPct = (String) xPath.compile(".//*[local-name()='TaxSubtotal'][*[local-name()='TaxCategory']/*[local-name()='TaxScheme']/*[local-name()='ID']='01' or *[local-name()='TaxCategory']/*[local-name()='TaxScheme']/*[local-name()='Name']='IVA']/*[local-name()='Percent']/text()").evaluate(lineElem, XPathConstants.STRING);
                    }
                    if (itemIvaPct != null && !itemIvaPct.isBlank()) {
                        try {
                            itemFactura.setPorcentajeIva(new BigDecimal(itemIvaPct.trim()));
                        } catch (Exception ignored) {}
                    }

                    String itemIvaMonto = (String) xPath.compile(".//*[local-name()='TaxSubtotal'][*[local-name()='TaxCategory']/*[local-name()='TaxScheme']/*[local-name()='ID']='01' or *[local-name()='TaxCategory']/*[local-name()='TaxScheme']/*[local-name()='Name']='IVA']/*[local-name()='TaxAmount']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (itemIvaMonto != null && !itemIvaMonto.isBlank()) {
                        try {
                            itemFactura.setValorIva(new BigDecimal(itemIvaMonto.trim()));
                        } catch (Exception ignored) {}
                    }

                    String itemIncPct = (String) xPath.compile(".//*[local-name()='TaxSubtotal'][*[local-name()='TaxCategory']/*[local-name()='TaxScheme']/*[local-name()='ID']='04' or *[local-name()='TaxCategory']/*[local-name()='TaxScheme']/*[local-name()='Name']='INC']/*[local-name()='TaxCategory']/*[local-name()='Percent']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (itemIncPct != null && !itemIncPct.isBlank()) {
                        try {
                            itemFactura.setPorcentajeImpoconsumo(new BigDecimal(itemIncPct.trim()));
                        } catch (Exception ignored) {}
                    }

                    String itemIncMonto = (String) xPath.compile(".//*[local-name()='TaxSubtotal'][*[local-name()='TaxCategory']/*[local-name()='TaxScheme']/*[local-name()='ID']='04' or *[local-name()='TaxCategory']/*[local-name()='TaxScheme']/*[local-name()='Name']='INC']/*[local-name()='TaxAmount']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (itemIncMonto != null && !itemIncMonto.isBlank()) {
                        try {
                            itemFactura.setValorImpoconsumo(new BigDecimal(itemIncMonto.trim()));
                        } catch (Exception ignored) {}
                    }

                    String lineSubtotalStr = (String) xPath.compile(".//*[local-name()='LineExtensionAmount']/text()").evaluate(lineElem, XPathConstants.STRING);
                    if (lineSubtotalStr != null && !lineSubtotalStr.isBlank()) {
                        try {
                            itemFactura.setValorSubtotal(new BigDecimal(lineSubtotalStr.trim()));
                        } catch (Exception ignored) {}
                    }

                    // Total de la línea (con impuestos si existe o calculando subtotal + iva + impoconsumo)
                    BigDecimal totalCalculado = itemFactura.getValorSubtotal() != null ? itemFactura.getValorSubtotal() : BigDecimal.ZERO;
                    if (itemFactura.getValorIva() != null) {
                        totalCalculado = totalCalculado.add(itemFactura.getValorIva());
                    }
                    if (itemFactura.getValorImpoconsumo() != null) {
                        totalCalculado = totalCalculado.add(itemFactura.getValorImpoconsumo());
                    }
                    itemFactura.setValorTotal(totalCalculado.compareTo(BigDecimal.ZERO) > 0 ? totalCalculado : itemFactura.getValorSubtotal());

                    factura.addItem(itemFactura);
                }
                LOGGER.info("✅ Se extrajeron {} ítems para la factura {}", factura.getItems().size(), factura.getNumeroFactura());
            }
        } catch (Exception e) {
            LOGGER.warn("⚠️ No se pudieron extraer ítems de factura: {}", e.getMessage());
        }

        factura.setEstado("RADICADO");
        factura.setFaseId(1L);
        factura.setObservacion("");

        // Documento XML de la Factura (Tipo 06 - XML DE FACTURA)
        Documento docXml = new Documento();
        docXml.setNombreOriginal(xmlFile.getName());
        docXml.setTamano(xmlFile.length());
        docXml.setCodigoEstado("01");
        docXml.setCodigoExtension("01");
        docXml.setCodigoTipo("06");
        docXml.setArchivoTemporal(xmlFile);
        factura.addDocumento(docXml);

        // Documento PDF de la Factura (Tipo 05 - PDF DE FACTURA)
        if (item.getArchivoPdf() != null && item.getArchivoPdf().exists()) {
            Documento docPdf = new Documento();
            docPdf.setNombreOriginal(item.getArchivoPdf().getName());
            docPdf.setTamano(item.getArchivoPdf().length());
            docPdf.setCodigoEstado("01");
            docPdf.setCodigoExtension("02");
            docPdf.setCodigoTipo("05");
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