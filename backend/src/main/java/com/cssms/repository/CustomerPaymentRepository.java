package com.cssms.repository;

import com.cssms.entity.CustomerPayment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, Long>, JpaSpecificationExecutor<CustomerPayment> {

    boolean existsBySalesOrderIdAndAdvanceFalse(Long salesOrderId);

    List<CustomerPayment> findBySalesOrderId(Long salesOrderId);

    @Query("select coalesce(sum(p.amountReceived), 0) from CustomerPayment p where p.paymentDate between :from and :to")
    BigDecimal sumBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
