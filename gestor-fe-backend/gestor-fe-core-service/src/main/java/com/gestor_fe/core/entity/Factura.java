package com.gestor_fe.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "factura", schema = "gestor")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 15)
    private String nit;

    @Column(name = "numero_factura", nullable = false, length = 30)
    private String numeroFactura;

    @Column(nullable = false, length = 1200)
    private String cufe;

    @Column(name = "identificador_cargue", nullable = false)
    private Long identificadorCargue;

    @Column(nullable = false)
    private Long linea;

    // Campos de negocio adicionales sugeridos
    @Column(name = "razon_social_emisor")
    private String razonSocialEmisor;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    // Relación lógica con el PDF y XML guardados
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "documento_xml_id")
    private Documento documentoXml;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "documento_pdf_id")
    private Documento documentoPdf;
}