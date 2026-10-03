package ru.sportorg.organizations;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AthleteMembershipWrite(
        @NotBlank @Email @Size(max = 320) String email) {
}
