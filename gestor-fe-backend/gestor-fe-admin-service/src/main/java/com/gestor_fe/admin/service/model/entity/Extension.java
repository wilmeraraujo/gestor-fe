package com.gestor_fe.admin.service.model.entity;

import com.service.common.entity.Global;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name="extension",schema="admin")
public class Extension extends Global{

}
