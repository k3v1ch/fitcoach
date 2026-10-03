package ru.sportorg.trainings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/trainings")
class TrainingController {
    private final TrainingService service;

    TrainingController(TrainingService service) {
        this.service = service;
    }

    @GetMapping
    TrainingPage find(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) String q,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) UUID coachId,
            @RequestParam(required = false) UUID athleteId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.find(actor, organizationId, q, from, to, groupId, coachId, athleteId, status, page, size);
    }

    @PostMapping
    Training create(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @Valid @RequestBody TrainingWrite write) {
        return service.create(actor, organizationId, write);
    }

    @GetMapping("/{trainingId}")
    TrainingDetail get(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID trainingId) {
        return service.detail(actor, organizationId, trainingId);
    }

    @PatchMapping("/{trainingId}")
    Training patch(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID trainingId,
            @RequestBody TrainingPatch patch) {
        return service.patch(actor, organizationId, trainingId, patch);
    }

    @PutMapping("/{trainingId}/report")
    TrainingReport report(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID trainingId,
            @Valid @RequestBody TrainingReportWrite write) {
        return service.saveReport(actor, organizationId, trainingId, write);
    }

    @PutMapping("/{trainingId}/attendance")
    List<Attendance> attendance(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID trainingId,
            @RequestBody List<AttendanceWrite> items) {
        return service.saveAttendance(actor, organizationId, trainingId, items);
    }
}