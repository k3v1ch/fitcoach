package ru.sportorg.auth;

record PasswordResetEmailRequested(String email, String token) {
}