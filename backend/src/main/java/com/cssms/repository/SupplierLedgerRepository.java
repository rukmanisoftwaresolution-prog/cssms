package com.cssms.repository;

import com.cssms.entity.SupplierLedger;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierLedgerRepository extends JpaRepository<SupplierLedger, Long> {

    List<SupplierLedger> findBySupplierIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(Long supplierId, LocalDate from, LocalDate to);

    @Query("select coalesce(sum(l.debit), 0) from SupplierLedger l where l.supplier.id = :supplierId and l.txnDate < :date")
    BigDecimal sumDebitBefore(@Param("supplierId") Long supplierId, @Param("date") LocalDate date);

    @Query("select coalesce(sum(l.credit), 0) from SupplierLedger l where l.supplier.id = :supplierId and l.txnDate < :date")
    BigDecimal sumCreditBefore(@Param("supplierId") Long supplierId, @Param("date") LocalDate date);

    @Modifying(flushAutomatically = true)
    @Query("update SupplierLedger l set l.deleted = true where l.refType = :refType and l.refId = :refId")
    int softDeleteByRef(@Param("refType") String refType, @Param("refId") Long refId);
}
