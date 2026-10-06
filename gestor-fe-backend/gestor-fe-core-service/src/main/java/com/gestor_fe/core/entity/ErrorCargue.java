package com.gestor_fe.core.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "error_cargue", schema = "gestor")
public class ErrorCargue {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@Column(name = "numero_linea")
	private Integer numeroLinea;
	
	@Column(name = "tipo_error")
    private String tipoError;
	
    private String campo;
    
    private String error;
    
    @Column(name = "valor_asociado")
    private String valorAsociado;
    
    @Column(name = "cargue_id")
    private Long cargueId; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargue_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"errores", "facturas"})
    @ToString.Exclude
    private Cargue cargue;

    @Column(name = "created_at")  
    private LocalDateTime createdAt;
    
    @Column(name = "deleted_at")
    private LocalDate deletedAt;

}
