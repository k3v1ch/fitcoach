package ru.sportorg.reports;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@Service
class ReportService {
    private final ReportRepository repository;
    ReportService(ReportRepository repository) {
        this.repository = repository;
    }

    List<ReportTypeInfo> types(AuthenticatedUser actor, UUID org) {
        Membership membership = require(actor, org, "reports.read");
        List<ReportTypeInfo> result = new ArrayList<>();
        if (membership.permission("attendance.read")) {
            result.add(info(ReportType.ATTENDANCE, "Посещаемость"));
        }
        if (membership.permission("schedule.read")) {
            result.add(info(ReportType.TRAININGS, "Тренировки"));
        }
        if (membership.permission("progress.read")) {
            result.add(info(ReportType.PROGRESS, "Прогресс"));
        }
        if (membership.permission("charges.read")) {
            result.add(info(ReportType.CHARGES, "Начисления"));
        }
        return result;
    }

    ReportTable view(
            AuthenticatedUser actor,
            UUID org,
            ReportType type,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        requireSource(actor, org, type, "reports.read");
        validatePeriod(from, to);
        if (page < 0 || size < 1 || size > 100) {
            throw new OrganizationRequestException("Некорректная пагинация.");
        }
        List<Map<String, Object>> all = repository.rows(type, org, from, to, 10_000);
        int start = Math.min(page * size, all.size());
        int end = Math.min(start + size, all.size());
        List<String> columns = all.isEmpty() ? List.of() : new ArrayList<>(all.get(0).keySet());
        return new ReportTable(
                type,
                Instant.now(),
                columns,
                all.subList(start, end),
                page,
                size,
                all.size(),
                (int) Math.ceil((double) all.size() / size));
    }

    byte[] export(
            AuthenticatedUser actor,
            UUID org,
            ReportType type,
            String format,
            LocalDate from,
            LocalDate to) {
        Membership membership = requireSource(actor, org, type, "reports.read");
        if (!membership.permission("reports.export")) {
            throw new OrganizationPermissionException();
        }
        validatePeriod(from, to);
        if (!"CSV".equals(format)) {
            throw new OrganizationRequestException("Пока поддерживается только CSV.");
        }
        List<Map<String, Object>> rows = repository.rows(type, org, from, to, 10_001);
        if (rows.size() > 10_000) {
            throw new OrganizationRequestException("REPORT_TOO_LARGE: сузьте период или фильтры.");
        }
        return csv(rows);
    }

    private byte[] csv(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return new byte[0];
        }
        List<String> columns = new ArrayList<>(rows.get(0).keySet());
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", columns)).append('\n');
        for (Map<String, Object> row : rows) {
            csv.append(columns.stream()
                            .map(column -> escape(row.get(column)))
                            .collect(java.util.stream.Collectors.joining(",")))
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private ReportTypeInfo info(ReportType type, String name) {
        return new ReportTypeInfo(type, name, List.of("CSV"));
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new OrganizationRequestException("Укажите корректный период.");
        }
    }

    private Membership requireSource(AuthenticatedUser actor, UUID org, ReportType type, String permission) {
        Membership membership = require(actor, org, permission);
        String source = switch (type) {
            case ATTENDANCE -> "attendance.read";
            case TRAININGS -> "schedule.read";
            case PROGRESS -> "progress.read";
            case CHARGES -> "charges.read";
        };
        if (!membership.permission(source)) {
            throw new OrganizationPermissionException();
        }
        return membership;
    }

    private Membership require(AuthenticatedUser actor, UUID org, String permission) {
        if (actor == null || !repository.organizationExists(org)) {
            throw new OrganizationNotFoundException();
        }
        ReportRepository.Membership membership = repository.membership(actor.userId(), org);
        if (membership == null || !membership.permission(permission)) {
            throw new OrganizationPermissionException();
        }
        return new Membership(membership.permissions());
    }

    private record Membership(String permissions) {
        boolean permission(String value) {
            return permissions.contains(value);
        }
    }
}