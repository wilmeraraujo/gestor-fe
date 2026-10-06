package com.gestor_fe.core.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity
@Table(name = "factura_item", schema = "gestor")
public class FacturaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    @JsonIgnoreProperties({"documentos", "gestiones", "items"})
    @ToString.Exclude
    private Factura factura;

    @Column(name = "numero_linea")
    private Integer numeroLinea;

    @Column(name = "codigo_producto", length = 100)
    private String codigoProducto;

    @Column(name = "codigo_unspsc", length = 50)
    private String codigoUnspsc;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "cantidad", precision = 18, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "unidad_medida", length = 20)
    private String unidadMedida;

    @Column(name = "precio_unitario", precision = 18, scale = 4)
    private BigDecimal precioUnitario;

    @Column(name = "precio_referencia", precision = 18, scale = 4)
    private BigDecimal precioReferencia; // En caso de muestras o no remunerados

    @Column(name = "porcentaje_descuento", precision = 10, scale = 4)
    private BigDecimal porcentajeDescuento;

    @Column(name = "valor_descuento", precision = 18, scale = 2)
    private BigDecimal valorDescuento;

    @Column(name = "porcentaje_iva", precision = 10, scale = 4)
    private BigDecimal porcentajeIva;

    @Column(name = "valor_iva", precision = 18, scale = 2)
    private BigDecimal valorIva;

    @Column(name = "porcentaje_impoconsumo", precision = 10, scale = 4)
    private BigDecimal porcentajeImpoconsumo;

    @Column(name = "valor_impoconsumo", precision = 18, scale = 2)
    private BigDecimal valorImpoconsumo;

    @Column(name = "valor_subtotal", precision = 18, scale = 2)
    private BigDecimal valorSubtotal;

    @Column(name = "valor_total", precision = 18, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
