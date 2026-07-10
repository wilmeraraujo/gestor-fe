package com.gestor_fe.core.step;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.w3c.dom.Document;

import com.gestor_fe.core.dto.FacturaZipWrapperDto;
import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Factura;

public class FacturaZipProcessor implements ItemProcessor<FacturaZipWrapperDto, Factura> {

    private final Long identificadorCargue;
    private long currentLine = 0;

    public FacturaZipProcessor(Long identificadorCargue) {
        this.identificadorCargue = identificadorCargue;
    }

    @Override
    public Factura process(FacturaZipWrapperDto item) throws Exception {
        currentLine++;
        
        File xmlFile = item.getArchivoXml();
        if (xmlFile == null || !xmlFile.exists()) {
            return null; // Salta el registro si no se encuentra el XML base
        }

        // Cargar y parsear el documento XML de forma nativa (DOM)
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(false); // Desactivar Namespaces para facilitar búsquedas XPath directas
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        XPath xPath = XPathFactory.newInstance().newXPath();

        // 🔍 Extracción de datos del AttachedDocument de la DIAN mediante XPath
        String nitEmisor = (String) xPath.compile("//SenderParty//CompanyID/text()").evaluate(doc, XPathConstants.STRING);
        String razonSocial = (String) xPath.compile("//SenderParty//RegistrationName/text()").evaluate(doc, XPathConstants.STRING);
        String numeroFactura = (String) xPath.compile("//ParentDocumentID/text()").evaluate(doc, XPathConstants.STRING);
        String cufe = (String) xPath.compile("//UUID/text()").evaluate(doc, XPathConstants.STRING);
        String fechaStr = (String) xPath.compile("//IssueDate/text()").evaluate(doc, XPathConstants.STRING);
        String valorStr = (String) xPath.compile("//LegalMonetaryTotal/PayableAmount/text()").evaluate(doc, XPathConstants.STRING);

        // Instanciar y mapear la entidad de negocio Factura
        Factura factura = new Factura();
        factura.setNit(nitEmisor != null ? nitEmisor.trim() : "SIN_NIT");
        factura.setRazonSocialEmisor(razonSocial != null ? razonSocial.trim() : "DESCONOCIDO");
        factura.setNumeroFactura(numeroFactura != null ? numeroFactura.trim() : item.getNombreBase());
        factura.setCufe(cufe != null ? cufe.trim() : "SIN_CUFE");
        factura.setIdentificadorCargue(identificadorCargue);
        factura.setLinea(currentLine);

        if (fechaStr != null && !fechaStr.isBlank()) {
            factura.setFechaEmision(LocalDate.parse(fechaStr.trim()));
        }
        if (valorStr != null && !valorStr.isBlank()) {
            factura.setValorTotal(new BigDecimal(valorStr.trim()));
        }

        // 📂 Mapear metadatos del XML adjuntando el File temporal para el Writer
        Documento docXml = new Documento();
        docXml.setNombreOriginal(xmlFile.getName());
        docXml.setTamano(xmlFile.length());
        docXml.setEstadoId(1L);    
        docXml.setExtensionId(1L); // Catálogo: XML
        docXml.setTipoId(1L);      
        docXml.setArchivoTemporal(xmlFile); // Pasa la referencia física al Writer
        factura.setDocumentoXml(docXml);

        // 📂 Mapear metadatos del PDF si viene incluido en el paquete
        if (item.getArchivoPdf() != null && item.getArchivoPdf().exists()) {
            Documento docPdf = new Documento();
            docPdf.setNombreOriginal(item.getArchivoPdf().getName());
            docPdf.setTamano(item.getArchivoPdf().length());
            docPdf.setEstadoId(1L);
            docPdf.setExtensionId(2L); // Catálogo: PDF
            docPdf.setTipoId(1L);
            docPdf.setArchivoTemporal(item.getArchivoPdf()); // Pasa la referencia física al Writer
            factura.setDocumentoPdf(docPdf);
        }

        return factura;
    }
}