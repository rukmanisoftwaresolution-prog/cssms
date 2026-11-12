package com.cssms.repository;

import com.cssms.entity.SalesOrder;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    boolean existsByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SalesOrder s where s.id = :id")
    Optional<SalesOrder> findByIdForUpdate(@Param("id") Long id);

    @Query("select s from SalesOrder s join fetch s.customer where s.status = com.cssms.enums.DocStatus.ACTIVE")
    List<SalesOrder> findAllActiveWithCustomer();

    @Query("""
            select coalesce(sum(s.totalAmount), 0) from SalesOrder s
            where s.status = com.cssms.enums.DocStatus.ACTIVE and s.orderDate between :from and :to""")
    BigDecimal sumTotalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Revenue excluding GST and after discounts. */
    @Query("""
            select coalesce(sum(s.subTotal - s.discountTotal), 0) from SalesOrder s
            where s.status = com.cssms.enums.DocStatus.ACTIVE and s.orderDate between :from and :to""")
    BigDecimal sumRevenueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(s.pendingAmount), 0) from SalesOrder s
            where s.status = com.cssms.enums.DocStatus.ACTIVE""")
    BigDecimal totalPending();

    @Query("""
            select s.customer.customerName, sum(s.totalAmount) from SalesOrder s
            where s.status = com.cssms.enums.DocStatus.ACTIVE
            group by s.customer.id, s.customer.customerName order by sum(s.totalAmount) desc""")
    List<Object[]> topCustomers(Pageable pageable);
}
