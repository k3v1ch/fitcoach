package ru.sportorg.access;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MembershipPolicyTest {

    @Test
    void trainerCanReceiveAnyDeclaredPermission() {
        assertDoesNotThrow(() -> MembershipPolicy.validate(
                List.of("TRAINER"), List.of("members.read", "payments.write", "reports.export")));
    }

    @Test
    void agencyIsExclusiveAndReadOnly() {
        assertDoesNotThrow(() -> MembershipPolicy.validate(
                List.of("AGENCY"), List.of("finance.read", "reports.export")));
        assertThrows(MembershipPolicy.InvalidMembershipException.class,
                () -> MembershipPolicy.validate(List.of("AGENCY", "PARENT"), List.of("groups.read")));
        assertThrows(MembershipPolicy.InvalidMembershipException.class,
                () -> MembershipPolicy.validate(List.of("AGENCY"), List.of("groups.write")));
    }

    @Test
    void parentAndAthleteCannotReceiveUnrelatedPermissions() {
        assertDoesNotThrow(() -> MembershipPolicy.validate(
                List.of("PARENT", "ATHLETE"), List.of("athletes.read", "reports.export")));
        assertThrows(MembershipPolicy.InvalidMembershipException.class,
                () -> MembershipPolicy.validate(List.of("PARENT"), List.of("finance.read")));
        assertThrows(MembershipPolicy.InvalidMembershipException.class,
                () -> MembershipPolicy.validate(List.of("ATHLETE"), List.of("members.write")));
    }

        @Test
        void duplicateRolesAndPermissionsAreRejectedAsInvalidMemberships() {
                assertThrows(MembershipPolicy.InvalidMembershipException.class,
                                () -> MembershipPolicy.validate(List.of("TRAINER", "TRAINER"), List.of()));
                assertThrows(MembershipPolicy.InvalidMembershipException.class,
                                () -> MembershipPolicy.validate(List.of("TRAINER"), List.of("groups.read", "groups.read")));
        }
}