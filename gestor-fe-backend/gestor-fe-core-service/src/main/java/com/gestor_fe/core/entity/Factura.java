package com.gestor_fe.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity
@Table(name = "factura", schema = "gestor")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "razon_social_emisor")
    private String razonSocialEmisor;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    // =========================================================================
    // NUEVA RELACIÓN: Una factura tiene muchos documentos asociados
    // =========================================================================
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id") // Esto creará la llave foránea 'factura_id' en la tabla gestor.documento
    private List<Documento> documentos = new ArrayList<>();

    // Método helper conveniente para añadir documentos manteniendo consistencia
    public void addDocumento(Documento documento) {
        if (this.documentos == null) {
            this.documentos = new ArrayList<>();
        }
        this.documentos.add(documento);
    }
}