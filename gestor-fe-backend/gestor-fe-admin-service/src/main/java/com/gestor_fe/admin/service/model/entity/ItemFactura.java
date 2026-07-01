package com.gestor_fe.admin.service.model.entity;

import com.service.common.entity.Global;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "item_factura")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemFactura extends Global {

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private Double valor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;
}