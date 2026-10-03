package ru.sportorg.athletes;

import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.sportorg.auth.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
class ParentController {
    private final AthleteService service;

    ParentController(AthleteService service) {
        this.service = service;
    }

    @GetMapping("/parents")
    ParentPage parents(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID athleteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.parents(actor, organizationId, q, athleteId, page, size);
    }
}
