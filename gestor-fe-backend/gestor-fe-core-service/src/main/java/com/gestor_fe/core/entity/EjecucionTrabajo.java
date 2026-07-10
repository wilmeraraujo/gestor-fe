package com.gestor_fe.core.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "batch_job_execution")
public class EjecucionTrabajo{
	@Id
	@Column(name = "job_execution_id")
	private Long id;
	
	private String status;

}
