package ru.sportorg.auth;

public final class SessionExpiry {

    public static final String ATTRIBUTE = SessionExpiry.class.getName() + ".expiresAt";
    public static final long LIFETIME_MILLIS = 8 * 60 * 60 * 1000L;

    private SessionExpiry() {
    }
}