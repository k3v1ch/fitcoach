package ru.sportorg.organizations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationWrite(
        @NotBlank @Size(max = 200) String name,
        String description,
        String address,
        String timezone) {
}