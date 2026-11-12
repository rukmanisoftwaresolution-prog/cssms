package com.cssms.repository;

import com.cssms.entity.SupplierPayment;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long>, JpaSpecificationExecutor<SupplierPayment> {

    boolean existsByPurchaseOrderId(Long purchaseOrderId);

    @Query("select coalesce(sum(p.amountPaid), 0) from SupplierPayment p where p.paymentDate between :from and :to")
    BigDecimal sumBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
