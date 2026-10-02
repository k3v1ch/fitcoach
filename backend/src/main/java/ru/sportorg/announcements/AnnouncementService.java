package ru.sportorg.announcements;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@Service
class AnnouncementService {
    private final AnnouncementRepository repository;
    private final ObjectMapper mapper;
    private final Clock clock;

    AnnouncementService(AnnouncementRepository repository, ObjectMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    AnnouncementPage find(
        AuthenticatedUser actor, UUID org, String q, String status,
        boolean unread, Boolean requiresResponse, int page, int size
    ) {
        require(actor, org, "announcements.read");
        page(page, size);
        String query = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        long total = repository.count(org, query, actor.userId(), status, unread, requiresResponse);
        return new AnnouncementPage(
                repository.findPage(org, query, actor.userId(), status, unread, requiresResponse, size, page * size),
                page,
                size,
                total,
                pages(total, size));
    }

    Announcement get(AuthenticatedUser actor, UUID org, UUID id) {
        require(actor, org, "announcements.read");
        return repository.find(org, id, actor.userId()).orElseThrow(OrganizationNotFoundException::new);
    }

    @Transactional
    Announcement create(AuthenticatedUser actor, UUID org, AnnouncementWrite write) {
        requireTrainer(actor, org, "announcements.write");
        validateRecipients(org, write.recipientUserIds());
        if (write.requiresResponse() && write.responseDeadline() == null) {
            throw new OrganizationRequestException("Для объявления с ответом нужен срок.");
        }
        return repository.insert(org, write, actor.userId(), json(write.attachmentFileIds()), clock.instant());
    }

    @Transactional
    Announcement patch(AuthenticatedUser actor, UUID org, UUID id, AnnouncementWrite write, String status) {
        requireTrainer(actor, org, "announcements.write");
        Announcement current = repository.find(org, id, actor.userId())
                .orElseThrow(OrganizationNotFoundException::new);
        if (!"DRAFT".equals(current.status()) && !"ARCHIVED".equals(status)) {
            throw new OrganizationRequestException("После публикации объявление можно только архивировать.");
        }
        if (!List.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(status)) {
            throw new OrganizationRequestException("Недопустимый статус объявления.");
        }
        validateRecipients(org, write.recipientUserIds());
        repository.patch(org, id, write, status, clock.instant());
        return repository.find(org, id, actor.userId()).orElseThrow();
    }

    void read(AuthenticatedUser actor, UUID org, UUID id) {
        require(actor, org, "announcements.read");
        if (!repository.recipient(id, actor.userId())) {
            throw new OrganizationNotFoundException();
        }
        repository.markRead(id, actor.userId(), clock.instant());
    }

    @Transactional
    void respond(AuthenticatedUser actor, UUID org, UUID id, String response, String comment) {
        require(actor, org, "announcements.read");
        Announcement announcement = repository.find(org, id, actor.userId())
                .orElseThrow(OrganizationNotFoundException::new);
        if (!repository.recipient(id, actor.userId())
                || !announcement.requiresResponse()
                || !"PUBLISHED".equals(announcement.status())
                || announcement.responseDeadline() != null
                && !Instant.now(clock).isBefore(announcement.responseDeadline())
                || !List.of("ACCEPTED", "DECLINED").contains(response)) {
            throw new OrganizationRequestException("Ответ на объявление недоступен.");
        }
        repository.response(id, actor.userId(), response, comment, clock.instant());
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
        return new Access(membership.role("TRAINER") || membership.role("AGENCY"));
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

    private void validateRecipients(UUID org, List<UUID> users) {
        if (users == null
                || users.isEmpty()
                || users.size() != users.stream().distinct().count()
                || users.stream().anyMatch(user -> !repository.member(org, user))) {
            throw new OrganizationRequestException("Получатели должны быть участниками этой организации.");
        }
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new OrganizationRequestException("Некорректные вложения.");
        }
    }

    private void page(int p, int s) {
        if (p < 0 || s < 1 || s > 100) {
            throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
        }
    }

    private int pages(long t, int s) {
        return (int) Math.ceil((double) t / s);
    }
    private record Access(boolean manager) { }
    record AnnouncementPage(List<Announcement> items, int page, int size, long totalElements, int totalPages) { }
}