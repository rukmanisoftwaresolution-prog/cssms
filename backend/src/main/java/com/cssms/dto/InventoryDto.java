package com.cssms.dto;

import java.math.BigDecimal;

public final class InventoryDto {

    private InventoryDto() {
    }

    public record Response(
            Long id,
            Long materialId,
            String materialCode,
            String materialName,
            String unit,
            BigDecimal purchasedStock,
            BigDecimal soldStock,
            BigDecimal currentStock) {
    }
}
