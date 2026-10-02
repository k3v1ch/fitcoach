package ru.sportorg.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank @Email @Size(max = 320) String email,
        RegistrationAccountType accountType,
        @Size(max = 200) String fullName) {

    public RegistrationRequest {
        accountType = accountType == null ? RegistrationAccountType.ATHLETE : accountType;
    }
}