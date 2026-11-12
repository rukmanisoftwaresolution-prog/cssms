package com.cssms.service;

import com.cssms.dto.ExpenseDto.Request;
import com.cssms.dto.ExpenseDto.Response;
import com.cssms.dto.PageResponse;
import com.cssms.entity.Expense;
import com.cssms.enums.ExpenseType;
import com.cssms.exception.BadRequestException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.ExpenseRepository;
import com.cssms.service.FileStorageService.StoredFile;
import com.cssms.util.Pages;
import com.cssms.util.Specs;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "expenseDate", "expenseDate", "expenseType", "expenseType", "amount", "amount");

    private final ExpenseRepository repository;
    private final FileStorageService storageService;

    public record Attachment(Resource resource, String fileName, String contentType) {
    }

    @Transactional(readOnly = true)
    public PageResponse<Response> search(String search, ExpenseType type, LocalDate from, LocalDate to, Pageable pageable) {
        Pageable safe = Pages.safe(pageable, SORTS, Sort.by(Sort.Direction.DESC, "expenseDate", "id"));
        return PageResponse.of(repository.findAll(filter(search, type, from, to), safe).map(ExpenseService::toResponse));
    }

    public static Specification<Expense> filter(String search, ExpenseType type, LocalDate from, LocalDate to) {
        return Specification
                .where(Specs.<Expense>search(search, "description", "attachmentName"))
                .and(Specs.eq("expenseType", type))
                .and(Specs.between("expenseDate", from, to));
    }

    @Transactional(readOnly = true)
    public Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public Response create(Request request) {
        Expense expense = new Expense();
        apply(expense, request);
        return toResponse(repository.save(expense));
    }

    @Transactional
    public Response update(Long id, Request request) {
        Expense expense = find(id);
        apply(expense, request);
        return toResponse(expense);
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = find(id);
        expense.setDeleted(true);
    }

    @Transactional
    public Response attach(Long id, MultipartFile file) {
        Expense expense = find(id);
        StoredFile stored = storageService.store(file, "expenses");
        String previous = expense.getAttachmentPath();
        expense.setAttachmentName(stored.originalName());
        expense.setAttachmentPath(stored.relativePath());
        expense.setAttachmentContentType(stored.contentType());
        storageService.delete(previous);
        return toResponse(expense);
    }

    @Transactional(readOnly = true)
    public Attachment download(Long id) {
        Expense expense = find(id);
        if (expense.getAttachmentPath() == null) {
            throw new BadRequestException("Expense has no attachment");
        }
        return new Attachment(storageService.load(expense.getAttachmentPath()), expense.getAttachmentName(),
                expense.getAttachmentContentType());
    }

    @Transactional
    public Response removeAttachment(Long id) {
        Expense expense = find(id);
        storageService.delete(expense.getAttachmentPath());
        expense.setAttachmentName(null);
        expense.setAttachmentPath(null);
        expense.setAttachmentContentType(null);
        return toResponse(expense);
    }

    private Expense find(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Expense", id));
    }

    private void apply(Expense e, Request r) {
        e.setExpenseDate(r.expenseDate());
        e.setExpenseType(r.expenseType());
        e.setAmount(r.amount());
        e.setDescription(r.description());
    }

    static Response toResponse(Expense e) {
        return new Response(e.getId(), e.getExpenseDate(), e.getExpenseType(), e.getAmount(), e.getDescription(),
                e.getAttachmentName(), e.getAttachmentPath() != null);
    }
}
