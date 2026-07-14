package com.gestor_fe.core.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    
    @Column(name = "created_at")  
    private LocalDateTime createdAt;
    
    @Column(name = "deleted_at")
    private LocalDate deletedAt;

}
