package ru.sportorg.auth;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConfirmRegistrationRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 15, max = 128)
    private String password;

    @NotBlank
    @Size(max = 200)
    private String fullName;

    private RegistrationAccountType accountType = RegistrationAccountType.ATHLETE;

    private boolean fullNameProvided;

    public String getToken() {
        return token;
    }

    @JsonSetter("token")
    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    @JsonSetter("password")
    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    @JsonSetter("fullName")
    public void setFullName(String fullName) {
        this.fullName = fullName;
        this.fullNameProvided = true;
    }

    public RegistrationAccountType getAccountType() {
        return accountType;
    }

    @JsonSetter("accountType")
    public void setAccountType(RegistrationAccountType accountType) {
        this.accountType = accountType == null ? RegistrationAccountType.ATHLETE : accountType;
    }

    boolean isFullNameProvided() {
        return fullNameProvided;
    }
}