package com.gestor_fe.core.entity;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "documento", schema = "gestor")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1200)
    private String ruta;

    @Column(name = "nombre_original", nullable = false, length = 200)
    private String nombreOriginal;

    @Column(nullable = false)
    private Long tamano;

    @Column(name = "estado_id", nullable = false)
    private Long estadoId;

    @Column(name = "extension_id", nullable = false)
    private Long extensionId;

    @Column(name = "tipo_id", nullable = false)
    private Long tipoId;
    
 // Relación Opcional: Se llena si es un XML/PDF de Factura
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    @JsonIgnoreProperties("documentos") // 👈 Ignora la lista de documentos dentro de factura
    @ToString.Exclude // 👈 Evita StackOverflow en Lombok
    private Factura factura;

    // Relación Opcional: Se llena si es un Soporte del Prestador (RUT, Certificación, etc.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id")
    @JsonIgnoreProperties("soportes") // 👈 Ignora la lista de soportes dentro de prestador
    @ToString.Exclude
    private Prestador prestador;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp 
    private LocalDateTime createdAt;
    
    @Column(name = "deleted_at")
    private LocalDate deletedAt;
    
    @Transient
    private File archivoTemporal;
}