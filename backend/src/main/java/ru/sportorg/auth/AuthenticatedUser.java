package ru.sportorg.auth;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuthenticatedUser implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String normalizedEmail;
    private final String fullName;
    private final String passwordHash;
    private final String appRole;
    private final String status;
    private final boolean emailVerified;

    public AuthenticatedUser(UUID userId, String email, String normalizedEmail, String fullName,
                             String passwordHash, String appRole, String status, boolean emailVerified) {
        this.userId = userId;
        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.appRole = appRole;
        this.status = status;
        this.emailVerified = emailVerified;
    }

    public AuthenticatedUser(UUID userId, String email, String normalizedEmail, String fullName, String phone,
                             String passwordHash, String appRole, String status, boolean emailVerified) {
        this(userId, email, normalizedEmail, fullName, passwordHash, appRole, status, emailVerified);
    }

    public UUID userId() {
        return userId;
    }

    public String email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public String appRole() {
        return appRole;
    }

    String status() {
        return status;
    }

    boolean emailVerified() {
        return emailVerified;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + appRole));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return normalizedEmail;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"BLOCKED".equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status) && emailVerified;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AuthenticatedUser user && userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}