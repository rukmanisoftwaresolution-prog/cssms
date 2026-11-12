package com.cssms.controller;

import com.cssms.dto.ExpenseDto.Request;
import com.cssms.dto.ExpenseDto.Response;
import com.cssms.dto.PageResponse;
import com.cssms.enums.ExpenseType;
import com.cssms.security.Access;
import com.cssms.service.ExpenseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Expenses")
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@PreAuthorize(Access.ADMIN_MANAGER)
public class ExpenseController {

    private final ExpenseService service;

    @GetMapping
    public PageResponse<Response> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ExpenseType expenseType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        return service.search(search, expenseType, fromDate, toDate, pageable);
    }

    @GetMapping("/{id}")
    public Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public Response update(@PathVariable Long id, @Valid @RequestBody Request request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response attach(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return service.attach(id, file);
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
        ExpenseService.Attachment attachment = service.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(attachment.fileName()).build().toString())
                .body(attachment.resource());
    }

    @DeleteMapping("/{id}/attachment")
    public Response removeAttachment(@PathVariable Long id) {
        return service.removeAttachment(id);
    }
}
