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
    
    @Column(length = 50)
    private String estado;

    @Column(length = 1000)
    private String observacion;

    @Column(name = "fase_id")
    private Long faseId;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;
    
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    // =========================================================================
    // RELACIÓN OPTIMIZADA: Bidireccional mapeada por la entidad Documento
    // =========================================================================
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @SQLRestriction("deleted_at IS NULL")
    private List<Documento> documentos = new ArrayList<>();

    // Método helper optimizado
    public void addDocumento(Documento documento) {
        if (this.documentos == null) {
            this.documentos = new ArrayList<>();
        }
        this.documentos.add(documento);
        documento.setFactura(this); // 👈 Asigna la relación padre para la clave foránea
    }
}