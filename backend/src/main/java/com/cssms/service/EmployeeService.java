package com.cssms.service;

import com.cssms.dto.EmployeeDto.Request;
import com.cssms.dto.EmployeeDto.Response;
import com.cssms.dto.PageResponse;
import com.cssms.entity.Employee;
import com.cssms.enums.Status;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.EmployeeRepository;
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
public class EmployeeService {

    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "employeeCode", "employeeCode", "fullName", "fullName", "mobileNumber", "mobileNumber",
            "joiningDate", "joiningDate", "salary", "salary", "designation", "designation", "status", "status");

    private final EmployeeRepository repository;
    private final CodeGenerator codeGenerator;

    @Transactional(readOnly = true)
    public PageResponse<Response> search(String search, Status status, Pageable pageable) {
        Specification<Employee> spec = Specification
                .where(Specs.<Employee>search(search, "employeeCode", "fullName", "mobileNumber", "email", "designation"))
                .and(Specs.eq("status", status));
        Pageable safe = Pages.safe(pageable, SORTS, Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(repository.findAll(spec, safe).map(EmployeeService::toResponse));
    }

    @Transactional(readOnly = true)
    public Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public Response create(Request request) {
        Employee employee = new Employee();
        employee.setEmployeeCode(codeGenerator.code(Sequence.EMPLOYEE, "EMP-", 4));
        apply(employee, request);
        return toResponse(repository.save(employee));
    }

    @Transactional
    public Response update(Long id, Request request) {
        Employee employee = find(id);
        apply(employee, request);
        return toResponse(employee);
    }

    @Transactional
    public void delete(Long id) {
        find(id).setDeleted(true);
    }

    private Employee find(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Employee", id));
    }

    private void apply(Employee e, Request r) {
        e.setFullName(r.fullName());
        e.setMobileNumber(r.mobileNumber());
        e.setEmail(r.email());
        e.setAddress(r.address());
        e.setJoiningDate(r.joiningDate());
        e.setSalary(r.salary());
        e.setDesignation(r.designation());
        e.setStatus(r.status() != null ? r.status() : Status.ACTIVE);
    }

    static Response toResponse(Employee e) {
        return new Response(e.getId(), e.getEmployeeCode(), e.getFullName(), e.getMobileNumber(), e.getEmail(),
                e.getAddress(), e.getJoiningDate(), e.getSalary(), e.getDesignation(), e.getStatus());
    }
}
