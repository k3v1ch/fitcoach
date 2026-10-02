package ru.sportorg.announcements;

import java.util.UUID;

import jakarta.validation.Valid;
import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/announcements")
class AnnouncementController {
    private final AnnouncementService service;

    AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    @GetMapping
    AnnouncementService.AnnouncementPage find(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(required = false) Boolean requiresResponse,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.find(actor, organizationId, q, status, unread, requiresResponse, page, size);
    }

    @PostMapping
    Announcement create(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @Valid @RequestBody AnnouncementWrite write) {
        return service.create(actor, organizationId, write);
    }

    @GetMapping("/{announcementId}")
    Announcement get(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID announcementId) {
        return service.get(actor, organizationId, announcementId);
    }

    @PatchMapping("/{announcementId}")
    Announcement patch(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID announcementId,
            @RequestParam String status,
            @Valid @RequestBody AnnouncementWrite write) {
        return service.patch(actor, organizationId, announcementId, write, status);
    }

    @PutMapping("/{announcementId}/read")
    void read(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID announcementId) {
        service.read(actor, organizationId, announcementId);
    }

    @PostMapping("/{announcementId}/responses")
    void respond(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID announcementId,
            @RequestBody ResponseRequest request) {
        service.respond(actor, organizationId, announcementId, request.response(), request.comment());
    }

    record ResponseRequest(String response, String comment) { }
}