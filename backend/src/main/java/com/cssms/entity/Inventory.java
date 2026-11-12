package com.cssms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "inventory")
@SQLRestriction("deleted = false")
public class Inventory extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", unique = true)
    private Material material;

    @Column(name = "purchased_stock", nullable = false, precision = 14, scale = 3)
    private BigDecimal purchasedStock = BigDecimal.ZERO;

    @Column(name = "sold_stock", nullable = false, precision = 14, scale = 3)
    private BigDecimal soldStock = BigDecimal.ZERO;

    /** Current Stock = Purchased Stock - Sold Stock (kept in sync on every write). */
    @Column(name = "current_stock", nullable = false, precision = 14, scale = 3)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @PrePersist
    @PreUpdate
    void recalculate() {
        currentStock = purchasedStock.subtract(soldStock);
    }
}
