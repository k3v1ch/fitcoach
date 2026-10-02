package ru.sportorg.athletes;

import java.util.UUID;

import jakarta.validation.Valid;
import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/athletes")
class AthleteController {

    private final AthleteService service;

    AthleteController(AthleteService service) {
        this.service = service;
    }

    @GetMapping
    AthletePage find(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID organizationId,
                     @RequestParam(required = false) String q, @RequestParam(required = false) String status,
                     @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.find(actor, organizationId, q, status, page, size);
    }

    @PostMapping
    Athlete create(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID organizationId,
                   @Valid @RequestBody AthleteWrite write) {
        return service.create(actor, organizationId, write);
    }

    @GetMapping("/{athleteId}")
    Athlete get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID organizationId,
                @PathVariable UUID athleteId) {
        return service.get(actor, organizationId, athleteId);
    }

    @PatchMapping("/{athleteId}")
    Athlete patch(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID organizationId,
                  @PathVariable UUID athleteId, @RequestBody AthletePatch patch) {
        return service.patch(actor, organizationId, athleteId, patch);
    }
}