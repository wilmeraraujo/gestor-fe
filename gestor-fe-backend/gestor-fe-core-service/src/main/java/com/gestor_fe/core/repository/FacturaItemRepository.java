package com.gestor_fe.core.repository;

import com.gestor_fe.core.entity.FacturaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacturaItemRepository extends JpaRepository<FacturaItem, Long> {

    List<FacturaItem> findByFacturaIdAndDeletedAtIsNullOrderByNumeroLineaAsc(Long facturaId);
}
