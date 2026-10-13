package com.gestor_fe.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
@Table(name = "factura", schema = "gestor")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String nit;

    @Column(length = 2)
    private String dv;

    @Column(name = "primer_apellido", length = 100)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 100)
    private String segundoApellido;

    @Column(name = "primer_nombre", length = 100)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 100)
    private String segundoNombre;

    @Column(length = 255)
    private String direccion;

    @Column(name = "codigo_departamento", length = 10)
    private String codigoDepartamento;

    @Column(name = "codigo_municipio", length = 10)
    private String codigoMunicipio;

    @Column(name = "codigo_pais", length = 10)
    private String codigoPais;

    @Column(name = "numero_factura", nullable = false, length = 30)
    private String numeroFactura;

    @Column(nullable = false, length = 1200)
    private String cufe;

    @Column(name = "identificador_cargue", nullable = false)
    private Long identificadorCargue;

    @Column(nullable = false)
    private Long linea;

    @Column(name = "razon_social_emisor")
    private String razonSocialEmisor;

    @Column(name = "valor_subtotal")
    private BigDecimal valorSubtotal;

    @Column(name = "valor_iva")
    private BigDecimal valorIva;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    @Column(name = "codigo_tipo_documento", length = 50)
    private String codigoTipoDocumento; // 01 = Factura Venta, 02 = Exportación, 03/04 = Contingencia, 91 = NC, 92 = ND

    @Column(name = "codigo_tipo_operacion", length = 50)
    private String codigoTipoOperacion; // 10 = Estándar, 09 = AIU, 11 = Mandatos, etc.

    @Column(name = "codigo_ambiente_ejecucion", length = 50)
    private String codigoAmbienteEjecucion; // 1 = Producción, 2 = Pruebas

    @Column(length = 10)
    private String moneda; // COP, USD, EUR

    @Column(name = "tasa_cambio", precision = 18, scale = 4)
    private BigDecimal tasaCambio;

    @Column(name = "fecha_tasa_cambio")
    private LocalDate fechaTasaCambio;

    @Column(name = "codigo_tipo_identificacion_emisor", length = 50)
    private String codigoTipoIdentificacionEmisor; // 31 = NIT, 13 = CC, 22 = CE, 41 = Pasaporte

    @Column(name = "codigo_tipo_persona_emisor", length = 50)
    private String codigoTipoPersonaEmisor; // 1 = Jurídica, 2 = Natural

    @Column(name = "codigo_responsabilidad_fiscal_emisor", length = 150)
    private String codigoResponsabilidadFiscalEmisor; // O-13, O-15, O-23, O-47, R-99-PN

    @Column(name = "hora_emision", length = 30)
    private String horaEmision;

    @Column(name = "valor_bruto", precision = 18, scale = 2)
    private BigDecimal valorBruto; // Total valor bruto antes de tributos y descuentos (LineExtensionAmount)

    @Column(name = "total_descuentos", precision = 18, scale = 2)
    private BigDecimal totalDescuentos;

    @Column(name = "total_cargos", precision = 18, scale = 2)
    private BigDecimal totalCargos;

    @Column(name = "total_anticipos", precision = 18, scale = 2)
    private BigDecimal totalAnticipos;

    @Column(name = "valor_impoconsumo", precision = 18, scale = 2)
    private BigDecimal valorImpoconsumo; // Impoconsumo (Código 04)

    @Column(name = "otros_impuestos", precision = 18, scale = 2)
    private BigDecimal otrosImpuestos;

    // =========================================================================
    // 👤 DATOS DEL CLIENTE / ADQUIRENTE (AccountingCustomerParty)
    // =========================================================================
    @Column(name = "codigo_tipo_identificacion_cliente", length = 50)
    private String codigoTipoIdentificacionCliente;

    @Column(name = "codigo_tipo_persona_cliente", length = 50)
    private String codigoTipoPersonaCliente;

    @Column(name = "codigo_responsabilidad_fiscal_cliente", length = 150)
    private String codigoResponsabilidadFiscalCliente;

    @Column(name = "nit_cliente", length = 20)
    private String nitCliente;

    @Column(name = "dv_cliente", length = 2)
    private String dvCliente;

    @Column(name = "razon_social_cliente")
    private String razonSocialCliente;

    @Column(name = "direccion_cliente")
    private String direccionCliente;

    @Column(name = "ciudad_cliente", length = 100)
    private String ciudadCliente;

    @Column(name = "departamento_cliente", length = 100)
    private String departamentoCliente;

    @Column(name = "telefono_cliente", length = 50)
    private String telefonoCliente;

    @Column(name = "email_cliente")
    private String emailCliente;

    // =========================================================================
    // 💰 CONDICIONES COMERCIALES Y RETENCIONES
    // =========================================================================
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "codigo_forma_pago", length = 50)
    private String codigoFormaPago; // 1 = Contado, 2 = Crédito

    @Column(name = "codigo_medio_pago", length = 50)
    private String codigoMedioPago;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "valor_retefuente", precision = 18, scale = 2)
    private BigDecimal valorRetefuente;

    @Column(name = "valor_reteica", precision = 18, scale = 2)
    private BigDecimal valorReteica;

    @Column(name = "valor_reteiva", precision = 18, scale = 2)
    private BigDecimal valorReteiva;

    @Column(name = "total_retenciones", precision = 18, scale = 2)
    private BigDecimal totalRetenciones;
    
    // =========================================================================
    // 📌 ESTADO ACTUAL Y ULTIMA CAUSACIÓN (Para consultas rápidas en Grillas)
    // =========================================================================
    @Column(length = 50)
    private String estado;

    @Column(name = "fase_id")
    private Long faseId;

    @Column(length = 1000)
    private String observacion;

    @Column(name = "codigo_causal_devolucion", length = 50)
    private String codigoCausalDevolucion;

    @Column(name = "codigo_tipo_registro_contable", length = 50) // FC, GV, ORC, NI, TB
    private String codigoTipoRegistroContable;

    @Column(name = "codigo_movimiento", length = 50)
    private String codigoMovimiento;

    @Column(name = "numero_causacion", length = 50)
    private String numeroCausacion;

    @Column(name = "codigo_concepto", length = 50)
    private String codigoConcepto;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;
    
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    // =========================================================================
    // RELACIÓN 0: Ítems / Líneas Facturadas (Productos o Servicios)
    // =========================================================================
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @SQLRestriction("deleted_at IS NULL")
    private List<FacturaItem> items = new ArrayList<>();

    public void addItem(FacturaItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        item.setFactura(this);
    }

    // =========================================================================
    // RELACIÓN 1: Documentos (XML, PDF Factura, Soporte Causación, TB, Pago)
    // =========================================================================
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @SQLRestriction("deleted_at IS NULL")
    private List<Documento> documentos = new ArrayList<>();

    // =========================================================================
    // RELACIÓN 2: Historial de Gestión (Auditoría/Trazabilidad Completa)
    // =========================================================================
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @OrderBy("createdAt DESC") // Muestra siempre de la gestión más reciente a la más antigua
    @SQLRestriction("deleted_at IS NULL")
    private List<Gestion> gestiones = new ArrayList<>();

    // Helper para documentos
    public void addDocumento(Documento documento) {
        if (this.documentos == null) {
            this.documentos = new ArrayList<>();
        }
        this.documentos.add(documento);
        documento.setFactura(this);
    }

    // Helper para historial de gestiones
    public void addGestion(Gestion gestion) {
        if (this.gestiones == null) {
            this.gestiones = new ArrayList<>();
        }
        this.gestiones.add(gestion);
        gestion.setFactura(this);
    }

    // =========================================================================
    // 🔗 RELACIÓN 3: Lote / Evento de Cargue Origen
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identificador_cargue", insertable = false, updatable = false)
    @JsonIgnoreProperties({"facturas", "errores"})
    @ToString.Exclude
    private Cargue cargue;
}