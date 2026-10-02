package ru.sportorg.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CurrentUser(UUID userId, String email, String fullName, String appRole,
                          Instant expiresAt, List<OrganizationAccess> organizations) {

    public CurrentUser(UUID userId, String email, String fullName, String phone, String appRole,
                      Instant expiresAt, List<OrganizationAccess> organizations) {
        this(userId, email, fullName, appRole, expiresAt, organizations);
    }
}