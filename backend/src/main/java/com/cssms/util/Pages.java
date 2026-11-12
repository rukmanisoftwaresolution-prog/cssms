package com.cssms.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Maps client sort property names to entity paths; unknown properties are ignored (no 500s, no injection). */
public final class Pages {

    private Pages() {
    }

    public static Pageable safe(Pageable pageable, Map<String, String> allowedSorts, Sort defaultSort) {
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String mapped = allowedSorts.get(order.getProperty());
            if (mapped != null) {
                orders.add(new Sort.Order(order.getDirection(), mapped));
            }
        }
        Sort sort = orders.isEmpty() ? defaultSort : Sort.by(orders);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
