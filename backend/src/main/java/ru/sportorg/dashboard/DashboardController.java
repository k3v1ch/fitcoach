package ru.sportorg.dashboard;

import java.time.LocalDate;
import java.util.UUID;

import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/dashboard")
class DashboardController {
    private final DashboardService service;

    DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    Dashboard get(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam String view,
            @RequestParam(required = false) UUID athleteId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "0") int activityPage,
            @RequestParam(defaultValue = "20") int activitySize) {
        return service.get(actor, organizationId, view, athleteId, from, to, activityPage, activitySize);
    }
}