package com.cssms.service;

import com.cssms.dto.MaterialDto.Request;
import com.cssms.dto.MaterialDto.Response;
import com.cssms.dto.PageResponse;
import com.cssms.entity.Inventory;
import com.cssms.entity.Material;
import com.cssms.exception.BusinessException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.InventoryRepository;
import com.cssms.repository.MaterialRepository;
import com.cssms.repository.PurchaseOrderItemRepository;
import com.cssms.repository.SalesOrderItemRepository;
import com.cssms.util.CodeGenerator;
import com.cssms.util.CodeGenerator.Sequence;
import com.cssms.util.Pages;
import com.cssms.util.Specs;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "materialCode", "materialCode", "materialName", "materialName", "unit", "unit");

    private final MaterialRepository repository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final PurchaseOrderItemRepository purchaseItemRepository;
    private final SalesOrderItemRepository salesItemRepository;
    private final CodeGenerator codeGenerator;

    @Transactional(readOnly = true)
    public PageResponse<Response> search(String search, Pageable pageable) {
        Specification<Material> spec = Specs.search(search, "materialCode", "materialName", "description");
        Pageable safe = Pages.safe(pageable, SORTS, Sort.by(Sort.Direction.ASC, "materialName"));
        return PageResponse.of(repository.findAll(spec, safe).map(MaterialService::toResponse));
    }

    @Transactional(readOnly = true)
    public Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public Response create(Request request) {
        if (repository.existsByMaterialNameIgnoreCase(request.materialName())) {
            throw new BusinessException("Material '" + request.materialName() + "' already exists");
        }
        Material material = new Material();
        material.setMaterialCode(codeGenerator.code(Sequence.MATERIAL, "MAT-", 4));
        apply(material, request);
        Material saved = repository.save(material);
        inventoryService.createFor(saved);
        return toResponse(saved);
    }

    @Transactional
    public Response update(Long id, Request request) {
        Material material = find(id);
        if (repository.existsByMaterialNameIgnoreCaseAndIdNot(request.materialName(), id)) {
            throw new BusinessException("Material '" + request.materialName() + "' already exists");
        }
        apply(material, request);
        return toResponse(material);
    }

    @Transactional
    public void delete(Long id) {
        Material material = find(id);
        if (purchaseItemRepository.existsByMaterialId(id) || salesItemRepository.existsByMaterialId(id)) {
            throw new BusinessException("Material is used in purchase/sales orders and cannot be deleted");
        }
        material.setDeleted(true);
        inventoryRepository.findByMaterialIdForUpdate(id).ifPresent((Inventory i) -> i.setDeleted(true));
    }

    private Material find(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Material", id));
    }

    private void apply(Material m, Request r) {
        m.setMaterialName(r.materialName());
        m.setDescription(r.description());
        m.setUnit(r.unit() != null ? r.unit().toUpperCase() : "TON");
    }

    static Response toResponse(Material m) {
        return new Response(m.getId(), m.getMaterialCode(), m.getMaterialName(), m.getDescription(), m.getUnit());
    }
}
