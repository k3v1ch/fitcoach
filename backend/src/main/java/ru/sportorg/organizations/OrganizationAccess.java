package ru.sportorg.organizations;

import java.util.List;
import java.util.UUID;

public record OrganizationAccess(UUID organizationId, String organizationName,
                                 List<String> roles, List<String> permissions) {
}