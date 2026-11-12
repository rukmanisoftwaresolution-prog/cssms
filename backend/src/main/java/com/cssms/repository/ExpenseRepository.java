package com.cssms.repository;

import com.cssms.entity.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.expenseDate between :from and :to")
    BigDecimal sumBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Rows: ExpenseType, total amount. */
    @Query("select e.expenseType, sum(e.amount) from Expense e where e.expenseDate between :from and :to group by e.expenseType")
    List<Object[]> totalsByType(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
