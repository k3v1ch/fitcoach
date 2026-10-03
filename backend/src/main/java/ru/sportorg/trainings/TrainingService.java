package ru.sportorg.trainings;

import java.time.Clock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
class TrainingService {
    private static final List<String> STATUSES = List.of("PLANNED", "COMPLETED", "CANCELLED");
    private static final List<String> ATTENDANCE_STATUSES = List.of("UNMARKED", "PRESENT", "SICK", "ABSENT");
    private final TrainingRepository repository;
    private final ObjectMapper mapper;
    private final Clock clock;

    TrainingService(TrainingRepository repository, ObjectMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    TrainingPage find(AuthenticatedUser actor, UUID org, String q, Instant from, Instant to, UUID groupId, UUID coachId, UUID athleteId, String status, int page, int size) {
        var membership = require(actor, org, "schedule.read");
        UUID scopeUserId = selfOnly(membership) ? actor.userId() : null; if (from == null || to == null || !from.isBefore(to)) throw new OrganizationRequestException("from и to должны задавать непустой диапазон."); validatePage(page, size); if (status != null && !STATUSES.contains(status)) throw new OrganizationRequestException("Недопустимый статус тренировки.");
        String query = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT); long total = repository.count(org, query, from, to, groupId, coachId, athleteId, status, scopeUserId); return new TrainingPage(repository.find(org, query, from, to, groupId, coachId, athleteId, status, scopeUserId, size, page * size), page, size, total, (int) Math.ceil((double) total / size));
    }

    // 6.8, №029: каждый раздел — по своему праву; родитель и спортсмен — только тренировки своих групп и свои отметки
    TrainingDetail detail(AuthenticatedUser actor, UUID org, UUID id) {
        if (actor == null || !repository.organizationExists(org)) throw new OrganizationNotFoundException();
        var membership = repository.membership(actor.userId(), org).orElseThrow(OrganizationNotFoundException::new);
        boolean schedule = membership.permission("schedule.read");
        boolean reports = membership.permission("trainingReports.read");
        boolean marks = membership.permission("attendance.read");
        if (!schedule && !reports && !marks) throw new OrganizationPermissionException();
        Training training = repository.findById(org, id).orElseThrow(OrganizationNotFoundException::new);
        boolean self = selfOnly(membership);
        if (self && !repository.visibleTo(org, id, actor.userId())) throw new OrganizationNotFoundException();
        List<Attendance> attendance = null;
        if (marks) {
            attendance = repository.attendanceWithParticipants(id);
            if (self) {
                java.util.Set<UUID> own = java.util.Set.copyOf(repository.ownAthleteIds(org, actor.userId()));
                attendance = attendance.stream().filter(item -> own.contains(item.athleteId())).toList();
            }
        }
        return new TrainingDetail(id, schedule ? training : null, reports ? repository.report(id).orElse(null) : null, attendance);
    }

    TrainingReport saveReport(AuthenticatedUser actor, UUID org, UUID id, TrainingReportWrite write) {
        requireTrainer(actor, org, "trainingReports.write"); Training training = repository.findById(org, id).orElseThrow(OrganizationNotFoundException::new);
        if ("CANCELLED".equals(training.status()) || "COMPLETED".equals(training.status())) throw new OrganizationRequestException("Для этой тренировки отчёт недоступен для изменения.");
        if (!List.of("DRAFT", "CLOSED").contains(write.status()) || blank(write.topic()) || blank(write.actualContent())) throw new OrganizationRequestException("Некорректный статус или содержание отчёта.");
        boolean close = "CLOSED".equals(write.status());
        if (close) {
            if (!Instant.now(clock).isAfter(training.endsAt())) throw new OrganizationRequestException("Отчёт можно закрыть только после окончания тренировки.");
            repository.snapshotAttendance(id, clock.instant());
            List<UUID> participants = repository.participantIds(id);
            List<Attendance> records = repository.attendance(id);
            if (records.size() < participants.size() || records.stream().anyMatch(item -> "UNMARKED".equals(item.status()))) throw new OrganizationRequestException("Перед закрытием отчёта отметьте всех участников.");
        }
        repository.saveReport(id, write, actor.userId(), clock.instant(), close); return repository.report(id).orElseThrow();
    }

    @Transactional List<Attendance> saveAttendance(AuthenticatedUser actor, UUID org, UUID id, List<AttendanceWrite> items) {
        requireTrainer(actor, org, "attendance.write"); Training training = repository.findById(org, id).orElseThrow(OrganizationNotFoundException::new);
        if ("CANCELLED".equals(training.status()) || "COMPLETED".equals(training.status())) throw new OrganizationRequestException("Посещаемость этой тренировки доступна только для чтения.");
        if (items == null) throw new OrganizationRequestException("items не может быть null.");
        repository.snapshotAttendance(id, clock.instant()); List<UUID> participants = repository.participantIds(id);
        if (items.stream().anyMatch(item -> item == null || item.athleteId() == null || !participants.contains(item.athleteId()) || !List.of("PRESENT", "SICK", "ABSENT").contains(item.status())) || items.stream().map(AttendanceWrite::athleteId).distinct().count() != items.size()) throw new OrganizationRequestException("Можно отмечать только участников выбранной группы.");
        for (AttendanceWrite item : items) repository.saveAttendance(id, item, actor.userId(), clock.instant());
        return repository.attendance(id);
    }

    @Transactional Training create(AuthenticatedUser actor, UUID org, TrainingWrite write) {
        requireTrainer(actor, org, "schedule.write");
        validateWrite(org, write);
        try {
            return repository.insert(org, write, json(write.plan()), clock.instant());
        } catch (DuplicateKeyException e) {
            throw new OrganizationRequestException("Тренировка не может быть сохранена.");
        }
    }

    @Transactional Training patch(AuthenticatedUser actor, UUID org, UUID id, TrainingPatch patch) {
        requireTrainer(actor, org, "schedule.write"); if (patch.isEmpty()) throw new OrganizationRequestException("PATCH должен содержать хотя бы одно поле."); Training current = repository.findById(org, id).orElseThrow(OrganizationNotFoundException::new); if (!"PLANNED".equals(current.status())) throw new OrganizationRequestException("Завершённую или отменённую тренировку нельзя изменить.");
        if (patch.has("status")) { if (!"CANCELLED".equals(patch.status()) || blank(patch.cancelReason())) throw new OrganizationRequestException("Для отмены укажите непустую причину."); }
        if (patch.has("title") && blank(patch.title())) throw new OrganizationRequestException("Название тренировки не может быть пустым.");
        if (patch.has("startsAt") && patch.startsAt() == null || patch.has("endsAt") && patch.endsAt() == null) throw new OrganizationRequestException("Время тренировки не может быть null.");
        if (patch.has("startsAt") && patch.has("endsAt") && !patch.startsAt().isBefore(patch.endsAt())) throw new OrganizationRequestException("endsAt должен быть позже startsAt.");
        if (patch.has("groupId") && !repository.groupActive(patch.groupId(), org)) throw new OrganizationRequestException("Группа не найдена или архивирована.");
        if (patch.has("coachIds")) validateCoaches(org, patch.coachIds());
        repository.patch(org, id, patch, patch.has("plan") ? json(patch.plan()) : "[]", clock.instant()); return repository.findById(org, id).orElseThrow();
    }

    private void validateWrite(UUID org, TrainingWrite write) {
        if (blank(write.title())
                || write.startsAt() == null
                || write.endsAt() == null
                || !write.startsAt().isBefore(write.endsAt())
                || write.plan() == null
                || write.plan().stream().anyMatch(
                stage -> stage == null || blank(stage.title()) || stage.durationMinutes() <= 0)
                || !repository.groupActive(write.groupId(), org)) {
            throw new OrganizationRequestException("Некорректные данные тренировки.");
        }
        validateCoaches(org, write.coachIds());
        long minutes = write.plan().stream().mapToLong(TrainingStage::durationMinutes).sum();
        if (minutes > java.time.Duration.between(write.startsAt(), write.endsAt()).toMinutes()) {
            throw new OrganizationRequestException("Этапы не могут быть длиннее тренировки.");
        }
    }

    private void validateCoaches(UUID org, List<UUID> coaches) {
        if (coaches == null
                || coaches.isEmpty()
                || coaches.size() != coaches.stream().distinct().count()
                || coaches.stream().anyMatch(id -> !repository.activeCoach(id, org))) {
            throw new OrganizationRequestException("Все тренеры должны иметь активную роль TRAINER.");
        }
    }

    AttendanceList attendanceJournal(AuthenticatedUser actor, UUID org, LocalDate from, LocalDate to, UUID athleteId, UUID groupId, String status, int page, int size) {
        var membership = require(actor, org, "attendance.read");
        if (from == null || to == null || from.isAfter(to)) {
            throw new OrganizationRequestException("Укажите период: from не позже to.");
        }
        if (status != null && !ATTENDANCE_STATUSES.contains(status)) {
            throw new OrganizationRequestException("Недопустимый статус посещения.");
        }
        validatePage(page, size);
        UUID scopeUserId = selfOnly(membership) ? actor.userId() : null;
        Instant now = clock.instant();
        long total = repository.journalCount(org, from, to, athleteId, groupId, status, scopeUserId, now);
        TrainingRepository.JournalCounts counts = repository.journalSummary(org, from, to, athleteId, groupId, scopeUserId, now);
        int marked = counts.present() + counts.sick() + counts.absent();
        BigDecimal percent = marked == 0 ? null
                : BigDecimal.valueOf(counts.present()).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(marked), 2, RoundingMode.HALF_UP);
        AttendanceSummary summary = new AttendanceSummary(from, to, counts.trainingCount(), counts.participantRecords(),
                counts.present(), counts.sick(), counts.absent(), counts.unmarked(), percent);
        return new AttendanceList(repository.journal(org, from, to, athleteId, groupId, status, scopeUserId, now, size, page * size),
                page, size, total, (int) Math.ceil((double) total / size), summary);
    }

    // Родитель и спортсмен без ролей тренера или ведомства видят только своих детей / себя
    private boolean selfOnly(TrainingRepository.MembershipAccess membership) {
        return (membership.role("PARENT") || membership.role("ATHLETE")) && !membership.role("TRAINER") && !membership.role("AGENCY");
    }

    private void requireTrainer(AuthenticatedUser actor, UUID org, String permission) {
        var membership = require(actor, org, permission);
        if (!membership.role("TRAINER")) {
            throw new OrganizationPermissionException();
        }
    }

    private TrainingRepository.MembershipAccess require(
            AuthenticatedUser actor,
            UUID org,
            String permission) {
        if (actor == null || !repository.organizationExists(org)) {
            throw new OrganizationNotFoundException();
        }
        var membership = repository.membership(actor.userId(), org)
                .orElseThrow(OrganizationNotFoundException::new);
        if (!membership.permission(permission)) {
            throw new OrganizationPermissionException();
        }
        return membership;
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new OrganizationRequestException("Некорректный план тренировки.");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
        }
    }
}