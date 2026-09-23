package com.gestor_fe.core.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Entity
@Table(name = "cargue", schema = "gestor")
public class Cargue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nombre_archivo")
    private String nombreArchivo;
    
    @Column(name = "nit_prestador")
    private String nitPrestador;
        
    @Column(name = "exite_error")
    private Boolean exiteError;
    
    @Column(name = "numero_registro")
    private Integer numeroRegistro;
    
    private String usuario;
    
    @Column(name = "job_execution_id")
    private Long jobExecutionId;
    
    @Column(name = "created_at")  
    private LocalDateTime createdAt;
    
    @Column(name = "deleted_at")
    private LocalDate deletedAt;
}