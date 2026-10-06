package com.gestor_fe.core.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
@Table(name = "gestion", schema = "gestor")
public class Gestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    @JsonIgnoreProperties({"documentos", "gestiones"}) 
    private Factura factura;

    @Column(name = "fase_id", nullable = false)
    private Long faseId;

    @Column(name = "accion", nullable = false, length = 20)
    private String accion; // APROBADO / RECHAZADO

    @Column(name = "estado_resultado", nullable = false, length = 50)
    private String estadoResultado; // EN GESTIÓN, CAUSADO, IMPUESTOS VERIFICADOS, PAGADO, ANULADO

    @Column(name = "tipo_registro_contable_id", length = 10)
    private Long tipoRegistroContableId;

    @Column(name = "numero_causacion", length = 50)
    private String numeroCausacion;

    @Column(name = "causal_devolucion_id")
    private Long causalDevolucionId;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "usuario", length = 100)
    private String usuario;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}