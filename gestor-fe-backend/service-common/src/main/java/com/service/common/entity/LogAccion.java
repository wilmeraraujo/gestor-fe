package com.service.common.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "log_acciones",schema = "logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAccion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "modulo", length = 50, nullable = false)
	private String modulo;

	@Column(name = "registro_id", nullable = false)
	private Long registroId;

	@Column(name = "accion", length = 20, nullable = false)
	private String accion;

	@Column(name = "observacion", columnDefinition = "TEXT")
	private String observacion;

	@Column(name = "usuario_username", length = 100)
	private String usuarioUsername;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime createdAt;

}
