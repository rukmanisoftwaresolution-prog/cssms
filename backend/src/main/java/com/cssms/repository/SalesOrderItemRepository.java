package com.cssms.repository;

import com.cssms.entity.SalesOrderItem;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

    boolean existsByMaterialId(Long materialId);

    /** Rows: materialId, quantity, amount (excluding GST) for active sales in the period. */
    @Query("""
            select i.material.id, sum(i.quantity), sum(i.amount) from SalesOrderItem i
            where i.salesOrder.status = com.cssms.enums.DocStatus.ACTIVE
              and i.salesOrder.orderDate between :from and :to
            group by i.material.id""")
    List<Object[]> totalsByMaterial(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select i.material.materialName, sum(i.amount) from SalesOrderItem i
            where i.salesOrder.status = com.cssms.enums.DocStatus.ACTIVE
            group by i.material.id, i.material.materialName order by sum(i.amount) desc""")
    List<Object[]> salesByMaterialName();
}
