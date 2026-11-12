package com.cssms.service;

import com.cssms.dto.InventoryDto;
import com.cssms.entity.Inventory;
import com.cssms.entity.Material;
import com.cssms.exception.BusinessException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.InventoryRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** All stock movements go through here; rows are locked to keep Current = Purchased - Sold consistent. */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository repository;

    @Transactional(readOnly = true)
    public List<InventoryDto.Response> list(String search) {
        String term = search == null ? "" : search.trim().toLowerCase();
        return repository.findAllWithMaterial().stream()
                .filter(i -> term.isEmpty()
                        || i.getMaterial().getMaterialName().toLowerCase().contains(term)
                        || i.getMaterial().getMaterialCode().toLowerCase().contains(term))
                .map(InventoryService::toResponse)
                .toList();
    }

    @Transactional
    public Inventory createFor(Material material) {
        Inventory inventory = new Inventory();
        inventory.setMaterial(material);
        return repository.save(inventory);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void addPurchase(Long materialId, BigDecimal quantity) {
        Inventory inventory = lock(materialId);
        inventory.setPurchasedStock(inventory.getPurchasedStock().add(quantity));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reversePurchase(Long materialId, BigDecimal quantity) {
        Inventory inventory = lock(materialId);
        BigDecimal remaining = inventory.getCurrentStock().subtract(quantity);
        if (remaining.signum() < 0) {
            throw new BusinessException("Cannot reverse purchase of " + inventory.getMaterial().getMaterialName()
                    + ": stock has already been sold (available " + inventory.getCurrentStock() + ")");
        }
        inventory.setPurchasedStock(inventory.getPurchasedStock().subtract(quantity));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void addSale(Long materialId, BigDecimal quantity) {
        Inventory inventory = lock(materialId);
        if (inventory.getCurrentStock().compareTo(quantity) < 0) {
            throw new BusinessException("Insufficient stock for " + inventory.getMaterial().getMaterialName()
                    + ": available " + inventory.getCurrentStock() + ", requested " + quantity);
        }
        inventory.setSoldStock(inventory.getSoldStock().add(quantity));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reverseSale(Long materialId, BigDecimal quantity) {
        Inventory inventory = lock(materialId);
        inventory.setSoldStock(inventory.getSoldStock().subtract(quantity));
    }

    private Inventory lock(Long materialId) {
        return repository.findByMaterialIdForUpdate(materialId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inventory for material", materialId));
    }

    static InventoryDto.Response toResponse(Inventory i) {
        Material m = i.getMaterial();
        return new InventoryDto.Response(i.getId(), m.getId(), m.getMaterialCode(), m.getMaterialName(), m.getUnit(),
                i.getPurchasedStock(), i.getSoldStock(), i.getCurrentStock());
    }
}
