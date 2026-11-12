package com.cssms.service;

import com.cssms.dto.PageResponse;
import com.cssms.dto.SupplierDto.Request;
import com.cssms.dto.SupplierDto.Response;
import com.cssms.entity.Supplier;
import com.cssms.enums.Status;
import com.cssms.exception.BusinessException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.PurchaseOrderRepository;
import com.cssms.repository.SupplierRepository;
import com.cssms.util.CodeGenerator;
import com.cssms.util.CodeGenerator.Sequence;
import com.cssms.util.Money;
import com.cssms.util.Pages;
import com.cssms.util.Specs;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "supplierCode", "supplierCode", "supplierName", "supplierName",
            "contactPerson", "contactPerson", "mobileNumber", "mobileNumber", "status", "status");

    private final SupplierRepository repository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CodeGenerator codeGenerator;

    @Transactional(readOnly = true)
    public PageResponse<Response> search(String search, Status status, Pageable pageable) {
        Specification<Supplier> spec = Specification
                .where(Specs.<Supplier>search(search, "supplierCode", "supplierName", "contactPerson", "mobileNumber", "gstNumber"))
                .and(Specs.eq("status", status));
        Pageable safe = Pages.safe(pageable, SORTS, Sort.by(Sort.Direction.DESC, "id"));
        var page = repository.findAll(spec, safe);
        Map<Long, BigDecimal> outstanding = outstanding(page.getContent());
        return PageResponse.of(page.map(s -> toResponse(s, outstanding.getOrDefault(s.getId(), BigDecimal.ZERO))));
    }

    @Transactional(readOnly = true)
    public Response get(Long id) {
        Supplier supplier = find(id);
        return toResponse(supplier, outstanding(List.of(supplier)).getOrDefault(id, BigDecimal.ZERO));
    }

    @Transactional
    public Response create(Request request) {
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(codeGenerator.code(Sequence.SUPPLIER, "SUP-", 4));
        apply(supplier, request);
        return toResponse(repository.save(supplier), BigDecimal.ZERO);
    }

    @Transactional
    public Response update(Long id, Request request) {
        Supplier supplier = find(id);
        apply(supplier, request);
        return toResponse(supplier, outstanding(List.of(supplier)).getOrDefault(id, BigDecimal.ZERO));
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = find(id);
        if (purchaseOrderRepository.existsBySupplierId(id)) {
            throw new BusinessException("Supplier has purchase orders and cannot be deleted. Mark it INACTIVE instead");
        }
        supplier.setDeleted(true);
    }

    private Supplier find(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
    }

    private Map<Long, BigDecimal> outstanding(List<Supplier> suppliers) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (suppliers.isEmpty()) {
            return result;
        }
        for (Object[] row : purchaseOrderRepository.outstandingBySupplier(suppliers.stream().map(Supplier::getId).toList())) {
            result.put((Long) row[0], Money.round((BigDecimal) row[1]));
        }
        return result;
    }

    private void apply(Supplier s, Request r) {
        s.setSupplierName(r.supplierName());
        s.setGstNumber(r.gstNumber());
        s.setPanNumber(r.panNumber());
        s.setContactPerson(r.contactPerson());
        s.setMobileNumber(r.mobileNumber());
        s.setAddress(r.address());
        s.setBankDetails(r.bankDetails());
        s.setStatus(r.status() != null ? r.status() : Status.ACTIVE);
    }

    private static Response toResponse(Supplier s, BigDecimal outstanding) {
        return new Response(s.getId(), s.getSupplierCode(), s.getSupplierName(), s.getGstNumber(), s.getPanNumber(),
                s.getContactPerson(), s.getMobileNumber(), s.getAddress(), s.getBankDetails(), s.getStatus(), outstanding);
    }
}
