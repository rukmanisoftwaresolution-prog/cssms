package com.cssms.service;

import com.cssms.dto.PageResponse;
import com.cssms.dto.SalesDto.CancelRequest;
import com.cssms.dto.SalesDto.ItemRequest;
import com.cssms.dto.SalesDto.ItemResponse;
import com.cssms.dto.SalesDto.Request;
import com.cssms.dto.SalesDto.Response;
import com.cssms.entity.Customer;
import com.cssms.entity.CustomerPayment;
import com.cssms.entity.Material;
import com.cssms.entity.SalesOrder;
import com.cssms.entity.SalesOrderItem;
import com.cssms.enums.DocStatus;
import com.cssms.enums.PaymentMode;
import com.cssms.enums.PaymentStatus;
import com.cssms.enums.Status;
import com.cssms.exception.BadRequestException;
import com.cssms.exception.BusinessException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.CustomerPaymentRepository;
import com.cssms.repository.CustomerRepository;
import com.cssms.repository.MaterialRepository;
import com.cssms.repository.SalesOrderRepository;
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
public class SalesOrderService {

    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "salesOrderNumber", "salesOrderNumber", "invoiceNumber", "invoiceNumber",
            "orderDate", "orderDate", "customerName", "customer.customerName", "totalAmount", "totalAmount",
            "pendingAmount", "pendingAmount", "paymentStatus", "paymentStatus", "status", "status",
            "createdAt", "createdAt");

    private final SalesOrderRepository repository;
    private final CustomerRepository customerRepository;
    private final MaterialRepository materialRepository;
    private final CustomerPaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final LedgerService ledgerService;
    private final CodeGenerator codeGenerator;

    @Transactional(readOnly = true)
    public PageResponse<Response> search(String search, Long customerId, Long materialId, DocStatus status,
                                         PaymentStatus paymentStatus, boolean pendingOnly, LocalDate fromDate,
                                         LocalDate toDate, Pageable pageable) {
        Pageable safe = Pages.safe(pageable, SORTS, Sort.by(Sort.Direction.DESC, "orderDate", "id"));
        return PageResponse.of(repository.findAll(filter(search, customerId, materialId, status, paymentStatus,
                pendingOnly, fromDate, toDate, null), safe).map(SalesOrderService::toResponse));
    }

    /** Shared by the list endpoint and the reports. */
    public static Specification<SalesOrder> filter(String search, Long customerId, Long materialId, DocStatus status,
                                                   PaymentStatus paymentStatus, boolean pendingOnly, LocalDate from,
                                                   LocalDate to, String invoiceNumber) {
        Specification<SalesOrder> spec = Specification
                .where(Specs.<SalesOrder>search(search, "salesOrderNumber", "invoiceNumber", "customer.customerName",
                        "vehicleNumber"))
                .and(Specs.eq("customer.id", customerId))
                .and(Specs.eq("status", status))
                .and(Specs.eq("paymentStatus", paymentStatus))
                .and(Specs.eq("invoiceNumber", invoiceNumber))
                .and(Specs.between("orderDate", from, to));
        if (pendingOnly) {
            spec = spec.and((root, q, cb) -> cb.and(cb.equal(root.get("status"), DocStatus.ACTIVE),
                    cb.greaterThan(root.<BigDecimal>get("pendingAmount"), BigDecimal.ZERO)));
        }
        if (materialId != null) {
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<SalesOrderItem> item = sub.from(SalesOrderItem.class);
                sub.select(item.get("id")).where(cb.equal(item.get("salesOrder"), root),
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
        Customer customer = lockActiveCustomer(request.customerId());
        long sequence = codeGenerator.next(Sequence.SALES_ORDER);
        SalesOrder order = new SalesOrder();
        order.setSalesOrderNumber(CodeGenerator.format("SO-", request.orderDate(), sequence));
        order.setInvoiceNumber(CodeGenerator.format("INV-", request.orderDate(), sequence));
        build(order, request, customer);
        SalesOrder saved = repository.saveAndFlush(order);
        postEffects(saved, customer, request);
        return toResponse(saved);
    }

    @Transactional
    public Response update(Long id, Request request) {
        SalesOrder order = lockEditable(id);
        reverseEffects(order);
        Customer customer = lockActiveCustomer(request.customerId());
        build(order, request, customer);
        SalesOrder saved = repository.saveAndFlush(order);
        postEffects(saved, customer, request);
        return toResponse(saved);
    }

    @Transactional
    public Response cancel(Long id, CancelRequest request) {
        SalesOrder order = lockEditable(id);
        reverseEffects(order);
        order.setStatus(DocStatus.CANCELLED);
        order.setCancelReason(request == null ? null : request.reason());
        order.setReceivedAmount(BigDecimal.ZERO);
        order.setPendingAmount(BigDecimal.ZERO);
        return toResponse(order);
    }

    private SalesOrder find(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Sales order", id));
    }

    private SalesOrder lockEditable(Long id) {
        SalesOrder order = repository.findByIdForUpdate(id).orElseThrow(() -> ResourceNotFoundException.of("Sales order", id));
        if (order.getStatus() == DocStatus.CANCELLED) {
            throw new BusinessException("Sales order " + order.getSalesOrderNumber() + " is already cancelled");
        }
        if (paymentRepository.existsBySalesOrderIdAndAdvanceFalse(id)) {
            throw new BusinessException("Sales order " + order.getSalesOrderNumber()
                    + " has customer payments. Delete the payments before editing or cancelling it");
        }
        return order;
    }

    private Customer lockActiveCustomer(Long customerId) {
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new BadRequestException("Customer not found: " + customerId));
        if (customer.getStatus() != Status.ACTIVE) {
            throw new BusinessException("Customer " + customer.getCustomerName() + " is inactive");
        }
        return customer;
    }

    private void build(SalesOrder order, Request request, Customer customer) {
        order.setCustomer(customer);
        order.setOrderDate(request.orderDate());
        order.setDeliveryAddress(request.deliveryAddress() != null ? request.deliveryAddress() : customer.getAddress());
        order.setVehicleNumber(request.vehicleNumber());
        order.setRemarks(request.remarks());
        order.getItems().clear();

        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal gst = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest line : request.items()) {
            Material material = materialRepository.findById(line.materialId())
                    .orElseThrow(() -> new BadRequestException("Material not found: " + line.materialId()));
            BigDecimal gross = Money.round(line.quantity().multiply(line.rate()));
            BigDecimal discount = Money.round(Money.nz(line.discount()));
            if (discount.compareTo(gross) > 0) {
                throw new BadRequestException("Discount for " + material.getMaterialName() + " exceeds the line amount");
            }
            SalesOrderItem item = new SalesOrderItem();
            item.setMaterial(material);
            item.setQuantity(line.quantity());
            item.setRate(Money.round(line.rate()));
            item.setDiscount(discount);
            item.setGstPercentage(Money.nz(line.gstPercentage()));
            item.setAmount(gross.subtract(discount));
            item.setGstAmount(Money.percentOf(item.getAmount(), item.getGstPercentage()));
            item.setTotalAmount(item.getAmount().add(item.getGstAmount()));
            order.addItem(item);
            subTotal = subTotal.add(gross);
            discountTotal = discountTotal.add(discount);
            gst = gst.add(item.getGstAmount());
            total = total.add(item.getTotalAmount());
        }
        BigDecimal advance = Money.round(Money.nz(request.advanceAmount()));
        if (advance.compareTo(total) > 0) {
            throw new BadRequestException("Advance amount cannot exceed the order total");
        }
        order.setSubTotal(subTotal);
        order.setDiscountTotal(discountTotal);
        order.setGstAmount(gst);
        order.setTotalAmount(total);
        order.setAdvanceAmount(advance);
        order.setReceivedAmount(advance);
        order.setPendingAmount(total.subtract(advance));
        order.setPaymentStatus(PurchaseOrderService.paymentStatusFor(advance, total));
    }

    private void postEffects(SalesOrder order, Customer customer, Request request) {
        quantityByMaterial(order).forEach(inventoryService::addSale);

        BigDecimal exposure = customer.getOutstandingAmount().add(order.getPendingAmount());
        if (customer.getCreditLimit().signum() > 0 && exposure.compareTo(customer.getCreditLimit()) > 0) {
            throw new BusinessException("Credit limit exceeded for " + customer.getCustomerName() + ": limit "
                    + customer.getCreditLimit() + ", outstanding would become " + exposure);
        }
        customer.setOutstandingAmount(exposure);
        ledgerService.postSale(order);

        if (order.getAdvanceAmount().signum() > 0) {
            CustomerPayment advance = new CustomerPayment();
            advance.setPaymentDate(order.getOrderDate());
            advance.setCustomer(customer);
            advance.setSalesOrder(order);
            advance.setPaymentMode(request.advancePaymentMode() != null ? request.advancePaymentMode() : PaymentMode.CASH);
            advance.setAmountReceived(order.getAdvanceAmount());
            advance.setRemarks("Advance with sales order");
            advance.setAdvance(true);
            CustomerPayment saved = paymentRepository.save(advance);
            ledgerService.postReceipt(saved);
        }
    }

    private void reverseEffects(SalesOrder order) {
        quantityByMaterial(order).forEach(inventoryService::reverseSale);
        Customer customer = customerRepository.findByIdForUpdate(order.getCustomer().getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", order.getCustomer().getId()));
        customer.setOutstandingAmount(customer.getOutstandingAmount().subtract(order.getPendingAmount()));
        ledgerService.reverseSale(order.getId());
        for (CustomerPayment payment : paymentRepository.findBySalesOrderId(order.getId())) {
            ledgerService.reverseReceipt(payment.getId());
            payment.setDeleted(true);
        }
    }

    private Map<Long, BigDecimal> quantityByMaterial(SalesOrder order) {
        Map<Long, BigDecimal> result = new TreeMap<>();
        for (SalesOrderItem item : order.getItems()) {
            result.merge(item.getMaterial().getId(), item.getQuantity(), BigDecimal::add);
        }
        return result;
    }

    static Response toResponse(SalesOrder so) {
        Set<String> names = new LinkedHashSet<>();
        BigDecimal quantity = BigDecimal.ZERO;
        for (SalesOrderItem i : so.getItems()) {
            names.add(i.getMaterial().getMaterialName());
            quantity = quantity.add(i.getQuantity());
        }
        var items = so.getItems().stream()
                .map(i -> new ItemResponse(i.getId(), i.getMaterial().getId(), i.getMaterial().getMaterialName(),
                        i.getQuantity(), i.getRate(), i.getDiscount(), i.getGstPercentage(), i.getAmount(),
                        i.getGstAmount(), i.getTotalAmount()))
                .toList();
        return new Response(so.getId(), so.getSalesOrderNumber(), so.getInvoiceNumber(), so.getOrderDate(),
                so.getCustomer().getId(), so.getCustomer().getCustomerName(), so.getDeliveryAddress(),
                so.getVehicleNumber(), String.join(", ", names), quantity, so.getSubTotal(), so.getDiscountTotal(),
                so.getGstAmount(), so.getTotalAmount(), so.getAdvanceAmount(), so.getReceivedAmount(),
                so.getPendingAmount(), so.getPaymentStatus(), so.getStatus(), so.getRemarks(), items,
                so.getCreatedAt());
    }
}
