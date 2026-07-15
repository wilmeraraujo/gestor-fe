package com.gestor_fe.core.entity;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "documento", schema = "gestor")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

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
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp 
    private LocalDateTime createdAt;
    
    @Column(name = "deleted_at")
    private LocalDate deletedAt;
    
    @Transient
    private File archivoTemporal;
}