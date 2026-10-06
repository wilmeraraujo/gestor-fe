package com.gestor_fe.admin.service.model.entity;

import com.service.common.entity.Global;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tipo_operacion", schema = "admin")
public class TipoOperacion extends Global {

}
