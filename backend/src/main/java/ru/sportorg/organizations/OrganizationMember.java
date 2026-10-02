package ru.sportorg.organizations;

import java.util.List;
import java.util.UUID;

public record OrganizationMember(UUID userId, String fullName, List<String> roles, String status) {
}