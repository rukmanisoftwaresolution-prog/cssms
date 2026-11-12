package com.cssms.repository;

import com.cssms.entity.PurchaseOrderItem;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    boolean existsByMaterialId(Long materialId);

    /** Rows: materialId, quantity, amount (excluding GST) for active POs in the period. */
    @Query("""
            select i.material.id, sum(i.quantity), sum(i.amount) from PurchaseOrderItem i
            where i.purchaseOrder.status = com.cssms.enums.DocStatus.ACTIVE
              and i.purchaseOrder.poDate between :from and :to
            group by i.material.id""")
    List<Object[]> totalsByMaterial(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
