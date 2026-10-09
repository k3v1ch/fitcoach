package ru.sportorg.announcements;

import ru.sportorg.notifications.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationConflictException;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@Service
class AnnouncementService {
    private final AnnouncementRepository repository;
    private final ObjectMapper mapper;
    private final Clock clock;

    private final NotificationService notifications;

    AnnouncementService(AnnouncementRepository repository, ObjectMapper mapper, Clock clock) {
        this(repository, mapper, clock, null);
    }

    @Autowired
    AnnouncementService(AnnouncementRepository repository, ObjectMapper mapper, Clock clock, NotificationService notifications) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
        this.notifications = notifications;
    }

    AnnouncementPage find(
        AuthenticatedUser actor, UUID org, String q, String status,
        boolean unread, Boolean requiresResponse, int page, int size
    ) {
        // Черновики видит только автор с announcements.write, опубликованное и архив — получатели,
        // тренер и ведомство (6.12, №047)
        Access access = require(actor, org, "announcements.read");
        page(page, size);
        String query = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        long total = repository.count(org, query, actor.userId(), status, unread, requiresResponse,
                access.writer(), access.manager());
        return new AnnouncementPage(
                repository.findPage(org, query, actor.userId(), status, unread, requiresResponse,
                        access.writer(), access.manager(), size, page * size),
                page,
                size,
                total,
                pages(total, size));
    }

    Announcement get(AuthenticatedUser actor, UUID org, UUID id) {
        Access access = require(actor, org, "announcements.read");
        Announcement announcement = repository.find(org, id, actor.userId()).orElseThrow(OrganizationNotFoundException::new);
        boolean visible = "DRAFT".equals(announcement.status())
                ? access.writer()
                : access.manager() || repository.recipient(id, actor.userId());
        if (!visible) throw new OrganizationNotFoundException();
        return announcement;
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
        if (!List.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(status)) {
            throw new OrganizationRequestException("Недопустимый статус объявления.");
        }
        Announcement current = repository.find(org, id, actor.userId())
                .orElseThrow(OrganizationNotFoundException::new);
        if ("ARCHIVED".equals(current.status())) {
            throw new OrganizationConflictException("Архивное объявление доступно только для чтения.");
        }
        if (!"DRAFT".equals(current.status())) {
            // После публикации текст, вложения и получатели зафиксированы — меняется только статус
            if (!"ARCHIVED".equals(status)) {
                throw new OrganizationRequestException("После публикации объявление можно только архивировать.");
            }
            repository.archive(org, id, clock.instant());
            return repository.find(org, id, actor.userId()).orElseThrow();
        }
        validateRecipients(org, write.recipientUserIds());
        if (write.requiresResponse() && write.responseDeadline() == null) {
            throw new OrganizationRequestException("Для объявления с ответом нужен срок.");
        }
        repository.patch(org, id, write, json(write.attachmentFileIds()), status, clock.instant());
        if (notifications != null && "PUBLISHED".equals(status)) {
            String text = write.title().trim() + (write.requiresResponse()
                    ? ". Нужен ответ до " + notifications.when(org, write.responseDeadline().toInstant()) + "." : ".");
            notifications.notifyUsers(org, write.recipientUserIds(), actor.userId(), "ANNOUNCEMENT_PUBLISHED",
                    "Новое объявление", text, "ANNOUNCEMENT", id);
        }
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

    // Аудитория объявления (6.12, №051): полный список получателей — только тренеру и ведомству
    AnnouncementAudiencePages.RecipientPage recipients(AuthenticatedUser actor, UUID org, UUID id, int page, int size) {
        Access access = require(actor, org, "announcements.read");
        if (!access.manager()) throw new OrganizationPermissionException();
        page(page, size);
        if (!repository.exists(org, id)) throw new OrganizationNotFoundException();
        long total = repository.countRecipients(id);
        return new AnnouncementAudiencePages.RecipientPage(repository.recipients(id, size, page * size), page, size, total, pages(total, size));
    }

    // История ответов (6.12, №054): тренер и ведомство — все ответы, получатель — только свои
    AnnouncementAudiencePages.ResponsePage responses(AuthenticatedUser actor, UUID org, UUID id, int page, int size) {
        Access access = require(actor, org, "announcements.read");
        page(page, size);
        if (!repository.exists(org, id)) throw new OrganizationNotFoundException();
        UUID onlyUser = access.manager() ? null : actor.userId();
        if (onlyUser != null && !repository.recipient(id, onlyUser)) throw new OrganizationNotFoundException();
        long total = repository.countResponses(id, onlyUser);
        return new AnnouncementAudiencePages.ResponsePage(repository.responses(id, onlyUser, size, page * size), page, size, total, pages(total, size));
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
        return new Access(membership.role("TRAINER") || membership.role("AGENCY"),
                membership.permission("announcements.write"));
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
    private record Access(boolean manager, boolean writer) { }
    record AnnouncementPage(List<Announcement> items, int page, int size, long totalElements, int totalPages) { }
}