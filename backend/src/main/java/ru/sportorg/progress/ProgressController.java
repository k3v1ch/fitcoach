package ru.sportorg.progress;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/athletes/{athleteId}")
class ProgressController {
    private final ProgressService service;

    ProgressController(ProgressService service) {
        this.service = service;
    }

    @GetMapping("/results")
    ResultPage results(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID athleteId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) String unit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.results(actor, organizationId, athleteId, from, to, metricName, unit, page, size);
    }

    @PostMapping("/results")
    Result create(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID athleteId,
            @Valid @RequestBody ResultWrite write) {
        return service.create(actor, organizationId, athleteId, write);
    }

    @PatchMapping("/results/{resultId}")
    Result patch(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID athleteId,
            @PathVariable UUID resultId,
            @RequestBody ResultPatch patch) {
        return service.patch(actor, organizationId, athleteId, resultId, patch);
    }

    @DeleteMapping("/results/{resultId}")
    ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID athleteId,
            @PathVariable UUID resultId) {
        service.delete(actor, organizationId, athleteId, resultId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/standards")
    ProgressService.PageData<AthleteStandard> standards(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID athleteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.standards(actor, organizationId, athleteId, page, size);
    }

    @GetMapping("/ranks")
    ProgressService.PageData<AthleteRank> ranks(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID athleteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.ranks(actor, organizationId, athleteId, page, size);
    }
}