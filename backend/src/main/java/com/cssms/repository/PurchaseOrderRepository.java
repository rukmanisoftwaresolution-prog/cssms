package com.cssms.repository;

import com.cssms.entity.PurchaseOrder;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    boolean existsBySupplierId(Long supplierId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PurchaseOrder p where p.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") Long id);

    @Query("select p from PurchaseOrder p join fetch p.supplier where p.status = com.cssms.enums.DocStatus.ACTIVE")
    List<PurchaseOrder> findAllActiveWithSupplier();

    @Query("""
            select coalesce(sum(p.totalAmount), 0) from PurchaseOrder p
            where p.status = com.cssms.enums.DocStatus.ACTIVE and p.poDate between :from and :to""")
    BigDecimal sumTotalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(p.amount), 0) from PurchaseOrder p
            where p.status = com.cssms.enums.DocStatus.ACTIVE and p.poDate between :from and :to""")
    BigDecimal sumAmountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(p.totalAmount - p.paidAmount), 0) from PurchaseOrder p
            where p.status = com.cssms.enums.DocStatus.ACTIVE""")
    BigDecimal totalOutstanding();

    @Query("""
            select p.supplier.id, sum(p.totalAmount - p.paidAmount) from PurchaseOrder p
            where p.status = com.cssms.enums.DocStatus.ACTIVE and p.supplier.id in :ids group by p.supplier.id""")
    List<Object[]> outstandingBySupplier(@Param("ids") Collection<Long> ids);

    @Query("""
            select p.supplier.supplierName, sum(p.totalAmount) from PurchaseOrder p
            where p.status = com.cssms.enums.DocStatus.ACTIVE
            group by p.supplier.id, p.supplier.supplierName order by sum(p.totalAmount) desc""")
    List<Object[]> topSuppliers(Pageable pageable);
}
