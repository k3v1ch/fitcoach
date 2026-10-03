package ru.sportorg.notifications;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.sportorg.auth.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/notifications")
class NotificationController {
    private final NotificationService service;

    NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    NotificationPage list(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(actor, organizationId, unread, page, size);
    }

    @PutMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void read(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID notificationId) {
        service.read(actor, organizationId, notificationId);
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void readAll(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID organizationId) {
        service.readAll(actor, organizationId);
    }
}
