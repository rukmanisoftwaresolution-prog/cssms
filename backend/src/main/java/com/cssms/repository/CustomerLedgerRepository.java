package com.cssms.repository;

import com.cssms.entity.CustomerLedger;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerLedgerRepository extends JpaRepository<CustomerLedger, Long> {

    List<CustomerLedger> findByCustomerIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(Long customerId, LocalDate from, LocalDate to);

    @Query("select coalesce(sum(l.debit), 0) from CustomerLedger l where l.customer.id = :customerId and l.txnDate < :date")
    BigDecimal sumDebitBefore(@Param("customerId") Long customerId, @Param("date") LocalDate date);

    @Query("select coalesce(sum(l.credit), 0) from CustomerLedger l where l.customer.id = :customerId and l.txnDate < :date")
    BigDecimal sumCreditBefore(@Param("customerId") Long customerId, @Param("date") LocalDate date);

    @Modifying(flushAutomatically = true)
    @Query("update CustomerLedger l set l.deleted = true where l.refType = :refType and l.refId = :refId")
    int softDeleteByRef(@Param("refType") String refType, @Param("refId") Long refId);
}
