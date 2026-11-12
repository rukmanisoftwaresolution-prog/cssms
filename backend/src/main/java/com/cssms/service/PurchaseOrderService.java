package com.cssms.service;

import com.cssms.dto.PageResponse;
import com.cssms.dto.PurchaseDto.CancelRequest;
import com.cssms.dto.PurchaseDto.ItemRequest;
import com.cssms.dto.PurchaseDto.ItemResponse;
import com.cssms.dto.PurchaseDto.Request;
import com.cssms.dto.PurchaseDto.Response;
import com.cssms.entity.Material;
import com.cssms.entity.PurchaseOrder;
import com.cssms.entity.PurchaseOrderItem;
import com.cssms.entity.Supplier;
import com.cssms.enums.DocStatus;
import com.cssms.enums.PaymentStatus;
import com.cssms.enums.Status;
import com.cssms.exception.BadRequestException;
import com.cssms.exception.BusinessException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.MaterialRepository;
import com.cssms.repository.PurchaseOrderRepository;
import com.cssms.repository.SupplierPaymentRepository;
import com.cssms.repository.SupplierRepository;
import com.cssms.util.CodeGenerator;
import com.cssms.util.CodeGenerator.Sequence;
import com.cssms.util.Money;
import com.cssms.util.Pages;
import com.cssms.util.Specs;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "poNumber", "poNumber", "poDate", "poDate", "supplierName", "supplier.supplierName",
            "totalAmount", "totalAmount", "paidAmount", "paidAmount", "paymentStatus", "paymentStatus",
            "status", "status", "createdAt", "createdAt");

    private final PurchaseOrderRepository repository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final SupplierPaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final LedgerService ledgerService;
    private final CodeGenerator codeGenerator;

    @Transactional(readOnly = true)
    public PageResponse<Response> search(String search, Long supplierId, Long materialId, PaymentStatus paymentStatus,
                                         DocStatus status, boolean pendingOnly, LocalDate fromDate, LocalDate toDate,
                                         Pageable pageable) {
        Pageable safe = Pages.safe(pageable, SORTS, Sort.by(Sort.Direction.DESC, "poDate", "id"));
        return PageResponse.of(repository.findAll(filter(search, supplierId, materialId, paymentStatus, status,
                pendingOnly, fromDate, toDate, null), safe).map(PurchaseOrderService::toResponse));
    }

    /** Shared by the list endpoint and the reports. */
    public static Specification<PurchaseOrder> filter(String search, Long supplierId, Long materialId,
                                                      PaymentStatus paymentStatus, DocStatus status,
                                                      boolean pendingOnly, LocalDate from, LocalDate to,
                                                      String poNumber) {
        Specification<PurchaseOrder> spec = Specification
                .where(Specs.<PurchaseOrder>search(search, "poNumber", "supplier.supplierName", "vehicleNumber"))
                .and(Specs.eq("supplier.id", supplierId))
                .and(Specs.eq("paymentStatus", paymentStatus))
                .and(Specs.eq("status", status))
                .and(Specs.eq("poNumber", poNumber))
                .and(Specs.between("poDate", from, to));
        if (pendingOnly) {
            spec = spec.and((root, q, cb) -> cb.and(cb.equal(root.get("status"), DocStatus.ACTIVE),
                    cb.notEqual(root.get("paymentStatus"), PaymentStatus.PAID)));
        }
        if (materialId != null) {
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<PurchaseOrderItem> item = sub.from(PurchaseOrderItem.class);
                sub.select(item.get("id")).where(cb.equal(item.get("purchaseOrder"), root),
                        cb.equal(item.get("material").get("id"), materialId));
                return cb.exists(sub);
            });
        }
        return spec;
    }

    @Transactional(readOnly = true)
    public Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public Response create(Request request) {
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(codeGenerator.documentNumber(Sequence.PURCHASE_ORDER, "PO-", request.poDate()));
        build(po, request);
        PurchaseOrder saved = repository.saveAndFlush(po);
        postEffects(saved);
        return toResponse(saved);
    }

    @Transactional
    public Response update(Long id, Request request) {
        PurchaseOrder po = lockEditable(id);
        reverseEffects(po);
        build(po, request);
        PurchaseOrder saved = repository.saveAndFlush(po);
        postEffects(saved);
        return toResponse(saved);
    }

    @Transactional
    public Response cancel(Long id, CancelRequest request) {
        PurchaseOrder po = lockEditable(id);
        reverseEffects(po);
        po.setStatus(DocStatus.CANCELLED);
        po.setCancelReason(request == null ? null : request.reason());
        return toResponse(po);
    }

    private PurchaseOrder lockEditable(Long id) {
        PurchaseOrder po = repository.findByIdForUpdate(id).orElseThrow(() -> ResourceNotFoundException.of("Purchase order", id));
        if (po.getStatus() == DocStatus.CANCELLED) {
            throw new BusinessException("Purchase order " + po.getPoNumber() + " is already cancelled");
        }
        if (paymentRepository.existsByPurchaseOrderId(id)) {
            throw new BusinessException("Purchase order " + po.getPoNumber()
                    + " has supplier payments. Delete the payments before editing or cancelling it");
        }
        return po;
    }

    private PurchaseOrder find(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Purchase order", id));
    }

    /** Copies request data and recalculates all amounts. Items are replaced wholesale. */
    private void build(PurchaseOrder po, Request request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new BadRequestException("Supplier not found: " + request.supplierId()));
        if (supplier.getStatus() != Status.ACTIVE) {
            throw new BusinessException("Supplier " + supplier.getSupplierName() + " is inactive");
        }
        po.setSupplier(supplier);
        po.setPoDate(request.poDate());
        po.setVehicleNumber(request.vehicleNumber());
        po.setRemarks(request.remarks());
        po.getItems().clear();

        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal gst = BigDecimal.ZERO;
        for (ItemRequest line : request.items()) {
            Material material = materialRepository.findById(line.materialId())
                    .orElseThrow(() -> new BadRequestException("Material not found: " + line.materialId()));
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setMaterial(material);
            item.setQuantity(line.quantity());
            item.setRatePerTon(Money.round(line.purchaseRatePerTon()));
            item.setGstPercentage(Money.nz(line.gstPercentage()));
            item.setAmount(Money.round(line.quantity().multiply(line.purchaseRatePerTon())));
            item.setGstAmount(Money.percentOf(item.getAmount(), item.getGstPercentage()));
            item.setTotalAmount(item.getAmount().add(item.getGstAmount()));
            po.addItem(item);
            amount = amount.add(item.getAmount());
            gst = gst.add(item.getGstAmount());
        }
        po.setAmount(amount);
        po.setGstAmount(gst);
        po.setTotalAmount(amount.add(gst));
        po.setPaidAmount(BigDecimal.ZERO);
        po.setPaymentStatus(PaymentStatus.PENDING);
    }

    private void postEffects(PurchaseOrder po) {
        quantityByMaterial(po).forEach(inventoryService::addPurchase);
        ledgerService.postPurchase(po);
    }

    private void reverseEffects(PurchaseOrder po) {
        quantityByMaterial(po).forEach(inventoryService::reversePurchase);
        ledgerService.reversePurchase(po.getId());
    }

    /** Sorted by material id so concurrent transactions lock inventory rows in the same order. */
    private Map<Long, BigDecimal> quantityByMaterial(PurchaseOrder po) {
        Map<Long, BigDecimal> result = new TreeMap<>();
        for (PurchaseOrderItem item : po.getItems()) {
            result.merge(item.getMaterial().getId(), item.getQuantity(), BigDecimal::add);
        }
        return result;
    }

    static PaymentStatus paymentStatusFor(BigDecimal paid, BigDecimal total) {
        if (paid.signum() == 0 && total.signum() > 0) {
            return PaymentStatus.PENDING;
        }
        return paid.compareTo(total) >= 0 ? PaymentStatus.PAID : PaymentStatus.PARTIAL_PAID;
    }

    static Response toResponse(PurchaseOrder po) {
        Set<String> names = new LinkedHashSet<>();
        BigDecimal quantity = BigDecimal.ZERO;
        for (PurchaseOrderItem i : po.getItems()) {
            names.add(i.getMaterial().getMaterialName());
            quantity = quantity.add(i.getQuantity());
        }
        var items = po.getItems().stream()
                .map(i -> new ItemResponse(i.getId(), i.getMaterial().getId(), i.getMaterial().getMaterialName(),
                        i.getQuantity(), i.getRatePerTon(), i.getGstPercentage(), i.getAmount(), i.getGstAmount(),
                        i.getTotalAmount()))
                .toList();
        return new Response(po.getId(), po.getPoNumber(), po.getPoDate(), po.getSupplier().getId(),
                po.getSupplier().getSupplierName(), po.getVehicleNumber(), String.join(", ", names), quantity,
                po.getAmount(), po.getGstAmount(), po.getTotalAmount(), po.getPaidAmount(), po.getBalanceAmount(),
                po.getPaymentStatus(), po.getStatus(), po.getRemarks(), items, po.getCreatedAt());
    }
}
