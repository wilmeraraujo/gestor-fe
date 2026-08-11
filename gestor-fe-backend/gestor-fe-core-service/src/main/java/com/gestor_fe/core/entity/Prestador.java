package com.gestor_fe.core.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.SQLRestriction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
@Table(name = "prestador", schema = "gestor")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Prestador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 15)
    private String nit;

    @Column(name = "razon_social", nullable = false, length = 1200)
    private String razonSocial;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(nullable = false, length = 10)
    private String telefono;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(name = "identificador_cargue", nullable = false)
    private Long identificadorCargue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Soportes del Prestador (RUT, Cámara de Comercio, etc.)
    @OneToMany(mappedBy = "prestador", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @SQLRestriction("deleted_at IS NULL")
    private List<Documento> soportes = new ArrayList<>();

    public void addSoporte(Documento doc) {
        soportes.add(doc);
        doc.setPrestador(this);
    }
}