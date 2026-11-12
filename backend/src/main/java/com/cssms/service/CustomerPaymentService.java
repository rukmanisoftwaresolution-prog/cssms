package com.cssms.service;

import com.cssms.dto.PageResponse;
import com.cssms.dto.PaymentDto.CustomerRequest;
import com.cssms.dto.PaymentDto.CustomerResponse;
import com.cssms.entity.Customer;
import com.cssms.entity.CustomerPayment;
import com.cssms.entity.SalesOrder;
import com.cssms.enums.DocStatus;
import com.cssms.enums.PaymentMode;
import com.cssms.exception.BadRequestException;
import com.cssms.exception.BusinessException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.CustomerPaymentRepository;
import com.cssms.repository.CustomerRepository;
import com.cssms.repository.SalesOrderRepository;
import com.cssms.util.Pages;
import com.cssms.util.Specs;
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
public class CustomerPaymentService {

    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "paymentDate", "paymentDate", "customerName", "customer.customerName",
            "invoiceNumber", "salesOrder.invoiceNumber", "paymentMode", "paymentMode",
            "amountReceived", "amountReceived");

    private final CustomerPaymentRepository repository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final LedgerService ledgerService;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(String search, Long customerId, PaymentMode mode, LocalDate from,
                                                 LocalDate to, Pageable pageable) {
        Pageable safe = Pages.safe(pageable, SORTS, Sort.by(Sort.Direction.DESC, "paymentDate", "id"));
        return PageResponse.of(repository.findAll(filter(search, customerId, mode, from, to), safe)
                .map(CustomerPaymentService::toResponse));
    }

    public static Specification<CustomerPayment> filter(String search, Long customerId, PaymentMode mode,
                                                        LocalDate from, LocalDate to) {
        return Specification
                .where(Specs.<CustomerPayment>search(search, "referenceNumber", "customer.customerName",
                        "salesOrder.invoiceNumber"))
                .and(Specs.eq("customer.id", customerId))
                .and(Specs.eq("paymentMode", mode))
                .and(Specs.between("paymentDate", from, to));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        SalesOrder order = salesOrderRepository.findByIdForUpdate(request.salesOrderId())
                .orElseThrow(() -> new BadRequestException("Invoice not found: " + request.salesOrderId()));
        if (order.getStatus() != DocStatus.ACTIVE) {
            throw new BusinessException("Invoice " + order.getInvoiceNumber() + " is cancelled");
        }
        if (!order.getCustomer().getId().equals(request.customerId())) {
            throw new BadRequestException("Invoice " + order.getInvoiceNumber() + " does not belong to the selected customer");
        }
        if (request.amountReceived().compareTo(order.getPendingAmount()) > 0) {
            throw new BusinessException("Amount exceeds the pending amount " + order.getPendingAmount()
                    + " of invoice " + order.getInvoiceNumber());
        }
        Customer customer = customerRepository.findByIdForUpdate(request.customerId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", request.customerId()));

        CustomerPayment payment = new CustomerPayment();
        payment.setPaymentDate(request.paymentDate());
        payment.setCustomer(customer);
        payment.setSalesOrder(order);
        payment.setPaymentMode(request.paymentMode());
        payment.setAmountReceived(request.amountReceived());
        payment.setReferenceNumber(request.referenceNumber());
        payment.setRemarks(request.remarks());
        CustomerPayment saved = repository.saveAndFlush(payment);

        applyToOrder(order, customer, request.amountReceived().negate());
        ledgerService.postReceipt(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        CustomerPayment payment = find(id);
        if (payment.isAdvance()) {
            throw new BusinessException("Advance receipts are managed through the sales order");
        }
        SalesOrder order = salesOrderRepository.findByIdForUpdate(payment.getSalesOrder().getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Sales order", payment.getSalesOrder().getId()));
        Customer customer = customerRepository.findByIdForUpdate(payment.getCustomer().getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", payment.getCustomer().getId()));
        applyToOrder(order, customer, payment.getAmountReceived());
        ledgerService.reverseReceipt(payment.getId());
        payment.setDeleted(true);
    }

    /** delta > 0 increases what the customer owes (payment reversal); delta < 0 decreases it. */
    private void applyToOrder(SalesOrder order, Customer customer, java.math.BigDecimal delta) {
        order.setPendingAmount(order.getPendingAmount().add(delta));
        order.setReceivedAmount(order.getTotalAmount().subtract(order.getPendingAmount()));
        order.setPaymentStatus(PurchaseOrderService.paymentStatusFor(order.getReceivedAmount(), order.getTotalAmount()));
        customer.setOutstandingAmount(customer.getOutstandingAmount().add(delta));
    }

    private CustomerPayment find(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Customer payment", id));
    }

    static CustomerResponse toResponse(CustomerPayment p) {
        return new CustomerResponse(p.getId(), p.getPaymentDate(), p.getCustomer().getId(),
                p.getCustomer().getCustomerName(), p.getSalesOrder().getId(), p.getSalesOrder().getInvoiceNumber(),
                p.getPaymentMode(), p.getAmountReceived(), p.getReferenceNumber(), p.getRemarks(), p.isAdvance());
    }
}
