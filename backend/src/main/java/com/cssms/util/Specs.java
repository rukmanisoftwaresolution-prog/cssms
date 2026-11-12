package com.cssms.util;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Small helpers for building filter specifications; null/blank filters are ignored. */
public final class Specs {

    private Specs() {
    }

    public static <T> Specification<T> all() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static <T> Specification<T> search(String term, String... fields) {
        if (term == null || term.isBlank()) {
            return all();
        }
        String pattern = "%" + escape(term.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String field : fields) {
                Expression<String> path = path(root, field).as(String.class);
                predicates.add(cb.like(cb.lower(path), pattern, '\\'));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static <T> Specification<T> eq(String field, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            return all();
        }
        return (root, query, cb) -> cb.equal(path(root, field), value);
    }

    public static <T> Specification<T> between(String field, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(path(root, field).as(LocalDate.class), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(path(root, field).as(LocalDate.class), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @SuppressWarnings("unchecked")
    private static <Y> Path<Y> path(From<?, ?> root, String dotted) {
        Path<?> current = root;
        for (String part : dotted.split("\\.")) {
            current = current.get(part);
        }
        return (Path<Y>) current;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
