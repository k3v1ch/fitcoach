package ru.sportorg.organizations;

import java.util.List;

public record OrganizationMemberPage(List<OrganizationMember> items, int page, int size,
                                     long totalElements, int totalPages) {
}