package com.cssms.repository;

import com.cssms.entity.Inventory;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.material.id = :materialId")
    Optional<Inventory> findByMaterialIdForUpdate(@Param("materialId") Long materialId);

    @Query("select i from Inventory i join fetch i.material m order by m.materialName")
    List<Inventory> findAllWithMaterial();

    @Query("select coalesce(sum(i.currentStock), 0) from Inventory i")
    BigDecimal totalCurrentStock();
}
