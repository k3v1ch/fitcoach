package ru.sportorg.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum RegistrationAccountType {
    TRAINER,
    ATHLETE,
    PARENT;

    @JsonCreator
    public static RegistrationAccountType from(String value) {
        if (value == null) {
            return ATHLETE;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return ATHLETE;
        }

        String upper = normalized.toUpperCase(Locale.ROOT);
        if ("ATHLET".equals(upper)) {
            return ATHLETE;
        }
        if ("TRENER".equals(upper)) {
            return TRAINER;
        }

        return RegistrationAccountType.valueOf(upper);
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
