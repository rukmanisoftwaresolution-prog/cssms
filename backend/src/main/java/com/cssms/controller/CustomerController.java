package com.cssms.controller;

import com.cssms.dto.CustomerDto.Request;
import com.cssms.dto.CustomerDto.Response;
import com.cssms.dto.PageResponse;
import com.cssms.enums.Status;
import com.cssms.security.Access;
import com.cssms.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customers")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @GetMapping
    @PreAuthorize(Access.ALL_ROLES)
    public PageResponse<Response> list(@RequestParam(required = false) String search,
                                       @RequestParam(required = false) Status status, Pageable pageable) {
        return service.search(search, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Access.ALL_ROLES)
    public Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize(Access.ALL_ROLES)
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.ALL_ROLES)
    public Response update(@PathVariable Long id, @Valid @RequestBody Request request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Access.ADMIN_MANAGER)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
