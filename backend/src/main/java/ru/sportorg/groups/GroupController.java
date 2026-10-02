package ru.sportorg.groups;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
class GroupController {
    private final GroupService service;

    GroupController(GroupService service) {
        this.service = service;
    }

    @GetMapping("/sections")
    SectionPage sections(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID sportTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.sections(actor, organizationId, q, sportTypeId, status, page, size);
    }

    @PostMapping("/sections")
    Section createSection(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @Valid @RequestBody SectionWrite write) {
        return service.createSection(actor, organizationId, write);
    }

    @PatchMapping("/sections/{sectionId}")
    Section patchSection(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID sectionId,
            @RequestBody SectionPatch patch) {
        return service.patchSection(actor, organizationId, sectionId, patch);
    }

    @GetMapping("/groups")
    GroupPage groups(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID coachId,
            @RequestParam(required = false) UUID athleteId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.groups(actor, organizationId, q, sectionId, coachId, athleteId, status, page, size);
    }

    @PostMapping("/groups")
    Group createGroup(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @Valid @RequestBody GroupWrite write) {
        return service.createGroup(actor, organizationId, write);
    }

    @GetMapping("/groups/{groupId}")
    GroupDetail detail(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "false") boolean includeFormer) {
        return service.detail(actor, organizationId, groupId, includeFormer);
    }

    @PatchMapping("/groups/{groupId}")
    Group patchGroup(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID groupId,
            @RequestBody GroupPatch patch) {
        return service.patchGroup(actor, organizationId, groupId, patch);
    }

    @PostMapping("/groups/{groupId}/athletes")
    GroupAthlete addAthlete(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID groupId,
            @RequestBody AddAthleteRequest request) {
        return service.addAthlete(actor, organizationId, groupId, request.athleteId(), request.joinedOn());
    }

    @PatchMapping("/groups/{groupId}/athletes/{athleteId}")
    GroupAthlete leaveAthlete(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID groupId,
            @PathVariable UUID athleteId,
            @RequestBody LeaveAthleteRequest request) {
        return service.leaveAthlete(actor, organizationId, groupId, athleteId, request.leftOn());
    }

    record AddAthleteRequest(UUID athleteId, LocalDate joinedOn) { }
    record LeaveAthleteRequest(LocalDate leftOn) { }
}