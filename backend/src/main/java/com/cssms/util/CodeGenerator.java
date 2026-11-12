package com.cssms.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** Generates business codes backed by PostgreSQL sequences (safe under concurrency). */
@Component
public class CodeGenerator {

    public enum Sequence {
        EMPLOYEE("seq_employee"),
        SUPPLIER("seq_supplier"),
        CUSTOMER("seq_customer"),
        MATERIAL("seq_material"),
        PURCHASE_ORDER("seq_purchase_order"),
        SALES_ORDER("seq_sales_order");

        private final String dbName;

        Sequence(String dbName) {
            this.dbName = dbName;
        }
    }

    @PersistenceContext
    private EntityManager entityManager;

    public String code(Sequence sequence, String prefix, int width) {
        return prefix + pad(next(sequence), width);
    }

    /** Document number such as PO-202610-00042. */
    public String documentNumber(Sequence sequence, String prefix, LocalDate date) {
        return format(prefix, date, next(sequence));
    }

    /** Formats an already drawn sequence value, so related documents (SO and INV) can share one counter. */
    public static String format(String prefix, LocalDate date, long value) {
        return prefix + date.format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + pad(value, 5);
    }

    public long next(Sequence sequence) {
        // Sequence names come from the enum above, never from user input.
        Number value = (Number) entityManager.createNativeQuery("select nextval('" + sequence.dbName + "')")
                .getSingleResult();
        return value.longValue();
    }

    private static String pad(long value, int width) {
        return String.format("%0" + width + "d", value);
    }
}
