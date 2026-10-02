package ru.sportorg.reports;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/reports")
class ReportController {
    private final ReportService service;

    ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/types")
    List<ReportTypeInfo> types(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId) {
        return service.types(actor, organizationId);
    }

    @GetMapping("/{reportType}")
    ReportTable view(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable ReportType reportType,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.view(actor, organizationId, reportType, from, to, page, size);
    }

    @GetMapping("/{reportType}/export")
    ResponseEntity<byte[]> export(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable ReportType reportType,
            @RequestParam String format,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        byte[] body = service.export(actor, organizationId, reportType, format, from, to);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(reportType.name().toLowerCase() + ".csv")
                                .build()
                                .toString())
                .body(body);
    }
}