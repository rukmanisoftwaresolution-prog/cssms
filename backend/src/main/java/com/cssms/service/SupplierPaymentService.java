package com.cssms.service;

import com.cssms.dto.PageResponse;
import com.cssms.dto.PaymentDto.SupplierRequest;
import com.cssms.dto.PaymentDto.SupplierResponse;
import com.cssms.entity.PurchaseOrder;
import com.cssms.entity.SupplierPayment;
import com.cssms.enums.DocStatus;
import com.cssms.enums.PaymentMode;
import com.cssms.exception.BadRequestException;
import com.cssms.exception.BusinessException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.PurchaseOrderRepository;
import com.cssms.repository.SupplierPaymentRepository;
import com.cssms.util.Pages;
import com.cssms.util.Specs;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierPaymentService {

    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "paymentDate", "paymentDate", "supplierName", "supplier.supplierName",
            "poNumber", "purchaseOrder.poNumber", "paymentMode", "paymentMode", "amountPaid", "amountPaid");

    private final SupplierPaymentRepository repository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final LedgerService ledgerService;

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> search(String search, Long supplierId, PaymentMode mode, LocalDate from,
                                                 LocalDate to, Pageable pageable) {
        Pageable safe = Pages.safe(pageable, SORTS, Sort.by(Sort.Direction.DESC, "paymentDate", "id"));
        return PageResponse.of(repository.findAll(filter(search, supplierId, mode, from, to), safe)
                .map(SupplierPaymentService::toResponse));
    }

    public static Specification<SupplierPayment> filter(String search, Long supplierId, PaymentMode mode,
                                                        LocalDate from, LocalDate to) {
        return Specification
                .where(Specs.<SupplierPayment>search(search, "referenceNumber", "supplier.supplierName",
                        "purchaseOrder.poNumber"))
                .and(Specs.eq("supplier.id", supplierId))
                .and(Specs.eq("paymentMode", mode))
                .and(Specs.between("paymentDate", from, to));
    }

    @Transactional(readOnly = true)
    public SupplierResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        PurchaseOrder order = purchaseOrderRepository.findByIdForUpdate(request.purchaseOrderId())
                .orElseThrow(() -> new BadRequestException("Purchase order not found: " + request.purchaseOrderId()));
        if (order.getStatus() != DocStatus.ACTIVE) {
            throw new BusinessException("Purchase order " + order.getPoNumber() + " is cancelled");
        }
        if (!order.getSupplier().getId().equals(request.supplierId())) {
            throw new BadRequestException("Purchase order " + order.getPoNumber() + " does not belong to the selected supplier");
        }
        if (request.amountPaid().compareTo(order.getBalanceAmount()) > 0) {
            throw new BusinessException("Amount exceeds the balance " + order.getBalanceAmount()
                    + " of purchase order " + order.getPoNumber());
        }
        SupplierPayment payment = new SupplierPayment();
        payment.setPaymentDate(request.paymentDate());
        payment.setSupplier(order.getSupplier());
        payment.setPurchaseOrder(order);
        payment.setPaymentMode(request.paymentMode());
        payment.setAmountPaid(request.amountPaid());
        payment.setReferenceNumber(request.referenceNumber());
        payment.setRemarks(request.remarks());
        SupplierPayment saved = repository.saveAndFlush(payment);

        applyToOrder(order, request.amountPaid());
        ledgerService.postSupplierPayment(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        SupplierPayment payment = find(id);
        PurchaseOrder order = purchaseOrderRepository.findByIdForUpdate(payment.getPurchaseOrder().getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Purchase order", payment.getPurchaseOrder().getId()));
        applyToOrder(order, payment.getAmountPaid().negate());
        ledgerService.reverseSupplierPayment(payment.getId());
        payment.setDeleted(true);
    }

    private void applyToOrder(PurchaseOrder order, BigDecimal delta) {
        order.setPaidAmount(order.getPaidAmount().add(delta));
        order.setPaymentStatus(PurchaseOrderService.paymentStatusFor(order.getPaidAmount(), order.getTotalAmount()));
    }

    private SupplierPayment find(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Supplier payment", id));
    }

    static SupplierResponse toResponse(SupplierPayment p) {
        return new SupplierResponse(p.getId(), p.getPaymentDate(), p.getSupplier().getId(),
                p.getSupplier().getSupplierName(), p.getPurchaseOrder().getId(), p.getPurchaseOrder().getPoNumber(),
                p.getPaymentMode(), p.getAmountPaid(), p.getReferenceNumber(), p.getRemarks());
    }
}
