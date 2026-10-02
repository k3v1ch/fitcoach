package ru.sportorg.events;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@Service
class EventService {
    private static final List<String> TYPES = List.of("CAMP", "COMPETITION", "MEDICAL_EXAM", "FUNDRAISER");
    private static final List<String> STATUSES = List.of("DRAFT", "PUBLISHED", "CANCELLED", "COMPLETED");
    private final EventRepository repository;
    private final ObjectMapper mapper;
    private final Clock clock;

    EventService(EventRepository repository, ObjectMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    EventPage find(
            AuthenticatedUser actor,
            UUID org,
            String q,
            LocalDate from,
            LocalDate to,
            String type,
            String status,
            int page,
            int size) {
        Access access = require(actor, org, "events.read");
        page(page, size);
        if (type != null && !TYPES.contains(type) || status != null && !STATUSES.contains(status)) {
            throw new OrganizationRequestException("Недопустимый тип или статус мероприятия.");
        }
        String query = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        UUID scope = access.self ? actor.userId() : null;
        String visibility = access.self ? "SELF" : null;
        long total = repository.count(org, query, from, to, type, status, scope, visibility);
        return new EventPage(
                repository.find(org, query, from, to, type, status, scope, visibility, size, page * size),
                page,
                size,
                total,
                pages(total, size));
    }

    Event get(AuthenticatedUser actor, UUID org, UUID id) {
        Access access = require(actor, org, "events.read");
        Event event = repository.findById(org, id).orElseThrow(OrganizationNotFoundException::new);
        if (access.self && !repository.eventVisible(id, actor.userId())) {
            throw new OrganizationNotFoundException();
        }
        return event;
    }

    @Transactional
    Event create(AuthenticatedUser actor, UUID org, EventWrite write) {
        requireTrainer(actor, org, "events.write");
        validateWrite(org, write);
        try {
            return repository.insert(org, write, actor.userId(), json(write.requiredDocumentTypes()), clock.instant());
        } catch (DuplicateKeyException e) {
            throw new OrganizationRequestException("Мероприятие не может быть сохранено.");
        }
    }

    @Transactional
    Event patch(AuthenticatedUser actor, UUID org, UUID id, EventPatch patch) {
        requireTrainer(actor, org, "events.write");
        Event current = repository.findById(org, id).orElseThrow(OrganizationNotFoundException::new);
        if (patch.empty()) {
            throw new OrganizationRequestException("PATCH должен содержать хотя бы одно поле.");
        }
        if (!"DRAFT".equals(current.status())
                && !(patch.has("status")
                && ("CANCELLED".equals(patch.status()) || "COMPLETED".equals(patch.status())))) {
            throw new OrganizationRequestException("Опубликованное мероприятие можно только завершить или отменить.");
        }
        if (patch.has("status") && !STATUSES.contains(patch.status())) {
            throw new OrganizationRequestException("Недопустимый статус мероприятия.");
        }
        if (patch.has("title") && blank(patch.title())) {
            throw new OrganizationRequestException("Название мероприятия не может быть пустым.");
        }
        repository.patch(
                org,
                id,
                patch,
                patch.has("requiredDocumentTypes") ? json(patch.requiredDocumentTypes()) : "[]",
                clock.instant());
        return repository.findById(org, id).orElseThrow();
    }

    @Transactional
    void addParticipants(AuthenticatedUser actor, UUID org, UUID eventId, List<UUID> athleteIds) {
        requireTrainer(actor, org, "events.write");
        Event event = repository.findById(org, eventId).orElseThrow(OrganizationNotFoundException::new);
        if (!List.of("DRAFT", "PUBLISHED").contains(event.status())
                || athleteIds == null
                || athleteIds.stream().anyMatch(id -> !repository.athleteExists(org, id))) {
            throw new OrganizationRequestException(
                    "Участники должны быть спортсменами этой организации и мероприятие должно быть опубликовано или черновиком.");
        }
        repository.addParticipants(eventId, athleteIds);
    }

    @Transactional
    void removeParticipant(AuthenticatedUser actor, UUID org, UUID eventId, UUID athleteId) {
        requireTrainer(actor, org, "events.write");
        Event event = repository.findById(org, eventId).orElseThrow(OrganizationNotFoundException::new);
        if (!List.of("DRAFT", "PUBLISHED").contains(event.status())) {
            throw new OrganizationRequestException("Участника нельзя удалить после завершения мероприятия.");
        }
        repository.removeParticipant(eventId, athleteId);
    }

    @Transactional
    void respond(AuthenticatedUser actor, UUID org, UUID eventId, UUID athleteId, String response, String comment) {
        require(actor, org, "events.read");
        if (!List.of("ACCEPTED", "DECLINED").contains(response) || !repository.participant(eventId, athleteId)) {
            throw new OrganizationRequestException("Некорректный ответ или участник не найден.");
        }
        Event event = repository.findById(org, eventId).orElseThrow(OrganizationNotFoundException::new);
        if (!"PUBLISHED".equals(event.status())
                || event.responseDeadline() != null && !Instant.now(clock).isBefore(event.responseDeadline())) {
            throw new OrganizationRequestException("Ответы на это мероприятие закрыты.");
        }
        if (!repository.athleteVisible(org, athleteId, actor.userId())) {
            throw new OrganizationNotFoundException();
        }
        repository.respond(eventId, athleteId, actor.userId(), response, comment, clock.instant());
    }

    List<EventParticipant> participants(AuthenticatedUser actor, UUID org, UUID eventId) {
        require(actor, org, "events.read");
        repository.findById(org, eventId).orElseThrow(OrganizationNotFoundException::new);
        return repository.participants(eventId);
    }

    private Access require(AuthenticatedUser actor, UUID org, String permission) {
        if (actor == null || !repository.organizationExists(org)) {
            throw new OrganizationNotFoundException();
        }
        var membership = repository.membership(actor.userId(), org)
                .orElseThrow(OrganizationNotFoundException::new);
        if (!membership.permission(permission)) {
            throw new OrganizationPermissionException();
        }
        return new Access(membership.role("PARENT") || membership.role("ATHLETE"));
    }

    private void requireTrainer(AuthenticatedUser actor, UUID org, String permission) {
        if (actor == null
                || !repository.organizationExists(org)
                || !repository.membership(actor.userId(), org)
                .map(membership -> membership.permission(permission) && membership.role("TRAINER"))
                .orElse(false)) {
            throw new OrganizationPermissionException();
        }
    }

    private void validateWrite(UUID org, EventWrite write) {
        if (blank(write.title())
                || !TYPES.contains(write.type())
                || !repository.sectionExists(org, write.sectionId())
                || write.requiredDocumentTypes() == null) {
            throw new OrganizationRequestException("Некорректные данные мероприятия.");
        }
        if ("FUNDRAISER".equals(write.type())
                && (write.targetAmount() == null
                || write.targetAmount().signum() <= 0
                || write.collectionDueOn() == null
                || write.startsAt() != null
                || write.endsAt() != null
                || write.location() != null)) {
            throw new OrganizationRequestException("Для FUNDRAISER нужны цель и срок сбора.");
        }
        if (!"FUNDRAISER".equals(write.type())
                && (write.startsAt() == null
                || write.endsAt() == null
                || !write.startsAt().isBefore(write.endsAt())
                || blank(write.location())
                || write.targetAmount() != null
                || write.collectionDueOn() != null)) {
            throw new OrganizationRequestException("Для обычного мероприятия нужны время и место.");
        }
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new OrganizationRequestException("Некорректные документы мероприятия.");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void page(int p, int s) {
        if (p < 0 || s < 1 || s > 100) {
            throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
        }
    }

    private int pages(long t, int s) {
        return (int) Math.ceil((double) t / s);
    }
    private record Access(boolean self) { }
}