package ru.sportorg.access;

import java.util.List;
import java.util.Set;

public final class MembershipPolicy {

    private static final Set<String> ROLES = Set.of("TRAINER", "PARENT", "ATHLETE", "AGENCY");
    private static final Set<String> READ_PERMISSIONS = Set.of(
            "members.read", "sections.read", "groups.read", "athletes.read", "schedule.read",
            "trainingReports.read", "attendance.read", "progress.read", "events.read",
            "announcements.read", "documents.read", "charges.read", "payments.read",
            "finance.read", "reports.read");
    private static final Set<String> WRITE_PERMISSIONS = Set.of(
            "organization.write", "members.write", "sections.write", "groups.write", "athletes.write",
            "schedule.write", "trainingReports.write", "attendance.write", "progress.write",
            "events.write", "announcements.write", "documents.write", "charges.write",
            "payments.write", "dictionaries.write");
    private static final Set<String> SELF_SERVICE_PERMISSIONS = Set.of(
            "sections.read", "groups.read", "athletes.read", "schedule.read", "trainingReports.read",
            "attendance.read", "progress.read", "events.read", "announcements.read", "documents.read",
            "charges.read", "payments.read", "reports.read", "reports.export");
    private static final Set<String> ALL_PERMISSIONS;

    static {
        var permissions = new java.util.HashSet<>(READ_PERMISSIONS);
        permissions.addAll(WRITE_PERMISSIONS);
        permissions.add("reports.export");
        ALL_PERMISSIONS = Set.copyOf(permissions);
    }

    private MembershipPolicy() { }

    public static void validate(List<String> roles, List<String> permissions) {
        if (roles == null || roles.isEmpty() || !ROLES.containsAll(roles)
            || roles.size() != new java.util.HashSet<>(roles).size()) {
            throw new InvalidMembershipException("Укажите допустимые роли без повторов.");
        }
        if (permissions == null || !ALL_PERMISSIONS.containsAll(permissions)
            || permissions.size() != new java.util.HashSet<>(permissions).size()) {
            throw new InvalidMembershipException("Указано недопустимое право или право повторяется.");
        }
        if (roles.contains("AGENCY") && roles.size() != 1) {
            throw new InvalidMembershipException("Роль AGENCY нельзя совмещать с другими ролями.");
        }

        Set<String> allowedPermissions = allowedPermissions(roles);
        if (!allowedPermissions.containsAll(permissions)) {
            throw new InvalidMembershipException("Права не соответствуют назначенным ролям.");
        }
    }

    public static List<String> selfServicePermissions() {
        return SELF_SERVICE_PERMISSIONS.stream().sorted().toList();
    }

    public static List<String> trainerPermissions() {
        return ALL_PERMISSIONS.stream().sorted().toList();
    }

    private static Set<String> allowedPermissions(List<String> roles) {
        if (roles.contains("TRAINER")) {
            return ALL_PERMISSIONS;
        }
        if (roles.contains("AGENCY")) {
            var permissions = new java.util.HashSet<>(READ_PERMISSIONS);
            permissions.add("reports.export");
            return Set.copyOf(permissions);
        }
        return SELF_SERVICE_PERMISSIONS;
    }

    public static final class InvalidMembershipException extends RuntimeException {

        public InvalidMembershipException(String message) {
            super(message);
        }
    }
}