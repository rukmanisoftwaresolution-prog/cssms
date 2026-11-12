package com.cssms.controller;

import com.cssms.dto.ReportParams;
import com.cssms.dto.ReportResponse;
import com.cssms.exception.BadRequestException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.security.Access;
import com.cssms.service.ExportService;
import com.cssms.service.ReportKey;
import com.cssms.service.ReportService;
import com.cssms.util.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize(Access.ALL_ROLES)
public class ReportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reportService;
    private final ExportService exportService;

    @GetMapping("/{key}")
    public ReportResponse report(@PathVariable String key, @ModelAttribute ReportParams params) {
        return reportService.generate(resolve(key), params);
    }

    @GetMapping("/{key}/export")
    public ResponseEntity<byte[]> export(@PathVariable String key,
                                         @RequestParam(defaultValue = "xlsx") String format,
                                         @ModelAttribute ReportParams params) {
        ReportResponse report = reportService.generate(resolve(key), params);
        boolean pdf = format.equalsIgnoreCase("pdf");
        if (!pdf && !format.equalsIgnoreCase("xlsx")) {
            throw new BadRequestException("format must be pdf or xlsx");
        }
        byte[] content = pdf ? exportService.toPdf(report) : exportService.toExcel(report);
        String fileName = key + "-" + LocalDate.now() + (pdf ? ".pdf" : ".xlsx");
        return ResponseEntity.ok()
                .contentType(pdf ? MediaType.APPLICATION_PDF : XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(content);
    }

    /** Resolves the report and enforces the per-report role matrix (sales executives see only some reports). */
    private ReportKey resolve(String key) {
        ReportKey reportKey = ReportKey.fromPath(key)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown report: " + key));
        boolean privileged = CurrentUser.hasRole("ADMIN") || CurrentUser.hasRole("MANAGER");
        if (!privileged && !reportKey.salesExecutiveAllowed()) {
            throw new AccessDeniedException("Report not permitted for this role");
        }
        return reportKey;
    }
}
