package ru.sportorg.trainings;

import java.time.LocalDate;
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
class AttendanceController {
    private final TrainingService service;

    AttendanceController(TrainingService service) {
        this.service = service;
    }

    @GetMapping("/attendance")
    AttendanceList journal(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) UUID athleteId,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.attendanceJournal(actor, organizationId, from, to, athleteId, groupId, status, page, size);
    }
}
