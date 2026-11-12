package com.cssms.service;

import com.cssms.dto.CustomerDto.Request;
import com.cssms.dto.CustomerDto.Response;
import com.cssms.dto.PageResponse;
import com.cssms.entity.Customer;
import com.cssms.enums.Status;
import com.cssms.exception.BusinessException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.CustomerRepository;
import com.cssms.repository.SalesOrderRepository;
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
public class CustomerService {

    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "customerCode", "customerCode", "customerName", "customerName", "companyName", "companyName",
            "mobileNumber", "mobileNumber", "creditLimit", "creditLimit", "outstandingAmount", "outstandingAmount",
            "status", "status");

    private final CustomerRepository repository;
    private final SalesOrderRepository salesOrderRepository;
    private final CodeGenerator codeGenerator;

    @Transactional(readOnly = true)
    public PageResponse<Response> search(String search, Status status, Pageable pageable) {
        Specification<Customer> spec = Specification
                .where(Specs.<Customer>search(search, "customerCode", "customerName", "companyName", "mobileNumber", "gstNumber"))
                .and(Specs.eq("status", status));
        Pageable safe = Pages.safe(pageable, SORTS, Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(repository.findAll(spec, safe).map(CustomerService::toResponse));
    }

    @Transactional(readOnly = true)
    public Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public Response create(Request request) {
        Customer customer = new Customer();
        customer.setCustomerCode(codeGenerator.code(Sequence.CUSTOMER, "CUST-", 4));
        apply(customer, request);
        return toResponse(repository.save(customer));
    }

    @Transactional
    public Response update(Long id, Request request) {
        Customer customer = find(id);
        apply(customer, request);
        return toResponse(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = find(id);
        if (salesOrderRepository.existsByCustomerId(id)) {
            throw new BusinessException("Customer has sales orders and cannot be deleted. Mark it INACTIVE instead");
        }
        customer.setDeleted(true);
    }

    private Customer find(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
    }

    private void apply(Customer c, Request r) {
        c.setCustomerName(r.customerName());
        c.setCompanyName(r.companyName());
        c.setGstNumber(r.gstNumber());
        c.setMobileNumber(r.mobileNumber());
        c.setEmail(r.email());
        c.setAddress(r.address());
        c.setCreditLimit(r.creditLimit());
        c.setStatus(r.status() != null ? r.status() : Status.ACTIVE);
    }

    static Response toResponse(Customer c) {
        return new Response(c.getId(), c.getCustomerCode(), c.getCustomerName(), c.getCompanyName(), c.getGstNumber(),
                c.getMobileNumber(), c.getEmail(), c.getAddress(), c.getCreditLimit(), c.getOutstandingAmount(),
                c.getStatus());
    }
}
