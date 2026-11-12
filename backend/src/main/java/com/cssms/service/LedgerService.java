package com.cssms.service;

import com.cssms.entity.CustomerLedger;
import com.cssms.entity.CustomerPayment;
import com.cssms.entity.PurchaseOrder;
import com.cssms.entity.SalesOrder;
import com.cssms.entity.SupplierLedger;
import com.cssms.entity.SupplierPayment;
import com.cssms.repository.CustomerLedgerRepository;
import com.cssms.repository.SupplierLedgerRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the double-entry style customer and supplier ledgers.
 * Customer ledger: debit = sale, credit = receipt. Supplier ledger: credit = purchase, debit = payment.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class LedgerService {

    static final String SALE = "SALE";
    static final String RECEIPT = "RECEIPT";
    static final String PURCHASE = "PURCHASE";
    static final String PAYMENT = "PAYMENT";

    private final CustomerLedgerRepository customerLedgerRepository;
    private final SupplierLedgerRepository supplierLedgerRepository;

    public void postSale(SalesOrder order) {
        CustomerLedger entry = new CustomerLedger();
        entry.setCustomer(order.getCustomer());
        entry.setTxnDate(order.getOrderDate());
        entry.setParticular("Sales Invoice " + order.getInvoiceNumber());
        entry.setDebit(order.getTotalAmount());
        entry.setCredit(BigDecimal.ZERO);
        entry.setRefType(SALE);
        entry.setRefId(order.getId());
        customerLedgerRepository.save(entry);
    }

    public void postReceipt(CustomerPayment payment) {
        CustomerLedger entry = new CustomerLedger();
        entry.setCustomer(payment.getCustomer());
        entry.setTxnDate(payment.getPaymentDate());
        entry.setParticular((payment.isAdvance() ? "Advance received (" : "Payment received (")
                + payment.getPaymentMode() + ") against " + payment.getSalesOrder().getInvoiceNumber());
        entry.setDebit(BigDecimal.ZERO);
        entry.setCredit(payment.getAmountReceived());
        entry.setRefType(RECEIPT);
        entry.setRefId(payment.getId());
        customerLedgerRepository.save(entry);
    }

    public void reverseSale(Long orderId) {
        customerLedgerRepository.softDeleteByRef(SALE, orderId);
    }

    public void reverseReceipt(Long paymentId) {
        customerLedgerRepository.softDeleteByRef(RECEIPT, paymentId);
    }

    public void postPurchase(PurchaseOrder order) {
        SupplierLedger entry = new SupplierLedger();
        entry.setSupplier(order.getSupplier());
        entry.setTxnDate(order.getPoDate());
        entry.setParticular("Purchase " + order.getPoNumber());
        entry.setDebit(BigDecimal.ZERO);
        entry.setCredit(order.getTotalAmount());
        entry.setRefType(PURCHASE);
        entry.setRefId(order.getId());
        supplierLedgerRepository.save(entry);
    }

    public void postSupplierPayment(SupplierPayment payment) {
        SupplierLedger entry = new SupplierLedger();
        entry.setSupplier(payment.getSupplier());
        entry.setTxnDate(payment.getPaymentDate());
        entry.setParticular("Payment made (" + payment.getPaymentMode() + ") against "
                + payment.getPurchaseOrder().getPoNumber());
        entry.setDebit(payment.getAmountPaid());
        entry.setCredit(BigDecimal.ZERO);
        entry.setRefType(PAYMENT);
        entry.setRefId(payment.getId());
        supplierLedgerRepository.save(entry);
    }

    public void reversePurchase(Long orderId) {
        supplierLedgerRepository.softDeleteByRef(PURCHASE, orderId);
    }

    public void reverseSupplierPayment(Long paymentId) {
        supplierLedgerRepository.softDeleteByRef(PAYMENT, paymentId);
    }
}
