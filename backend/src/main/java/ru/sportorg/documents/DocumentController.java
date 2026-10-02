package ru.sportorg.documents;

import java.time.LocalDate;
import java.util.UUID;

import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
class DocumentController {
    private final DocumentService service;

    DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    StoredFile uploadFile(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestPart MultipartFile file) {
        return service.uploadAttachment(actor, organizationId, file);
    }

    @DeleteMapping("/files/{fileId}")
    ResponseEntity<Void> deleteFile(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID fileId) {
        service.deleteFile(actor, organizationId, fileId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Document uploadDocument(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestPart MultipartFile file,
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam(required = false) UUID athleteId,
            @RequestParam(required = false) LocalDate issuedOn,
            @RequestParam(required = false) LocalDate validUntil) {
        return service.uploadDocument(actor, organizationId, file, title, type, athleteId, issuedOn, validUntil);
    }

    @GetMapping("/documents")
    DocumentPage documents(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID athleteId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.documents(actor, organizationId, q, athleteId, type, expired, page, size);
    }

    @GetMapping("/documents/{documentId}")
    Document document(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId) {
        return service.get(actor, organizationId, documentId);
    }

    @GetMapping("/files/{fileId}/content")
    ResponseEntity<Resource> content(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID fileId) {
        Resource resource = service.content(actor, organizationId, fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(resource.getFilename() == null ? "file" : resource.getFilename())
                                .build()
                                .toString())
                .body(resource);
    }
}