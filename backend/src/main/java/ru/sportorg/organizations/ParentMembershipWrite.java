package ru.sportorg.organizations;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParentMembershipWrite(
        @NotBlank @Email @Size(max = 320) String email) {
}