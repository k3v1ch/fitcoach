package ru.sportorg.events;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/events")
class EventController {
    private final EventService service;

    EventController(EventService service) {
        this.service = service;
    }

    @GetMapping
    EventPage find(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.find(actor, organizationId, q, from, to, type, status, page, size);
    }

    @PostMapping
    Event create(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @Valid @RequestBody EventWrite write) {
        return service.create(actor, organizationId, write);
    }

    @GetMapping("/{eventId}")
    Event get(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID eventId) {
        return service.get(actor, organizationId, eventId);
    }

    @PatchMapping("/{eventId}")
    Event patch(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID eventId,
            @RequestBody EventPatch patch) {
        return service.patch(actor, organizationId, eventId, patch);
    }

    @GetMapping("/{eventId}/participants")
    List<EventParticipant> participants(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID eventId) {
        return service.participants(actor, organizationId, eventId);
    }

    @PutMapping("/{eventId}/participants")
    void add(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID eventId,
            @RequestBody List<UUID> athleteIds) {
        service.addParticipants(actor, organizationId, eventId, athleteIds);
    }

    @DeleteMapping("/{eventId}/participants/{athleteId}")
    void remove(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID eventId,
            @PathVariable UUID athleteId) {
        service.removeParticipant(actor, organizationId, eventId, athleteId);
    }

    @PatchMapping("/{eventId}/participants/{athleteId}")
    void respond(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID eventId,
            @PathVariable UUID athleteId,
            @RequestBody ParticipantResponse request) {
        service.respond(actor, organizationId, eventId, athleteId, request.response(), request.comment());
    }

    record ParticipantResponse(String response, String comment) { }
}