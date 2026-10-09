package ru.sportorg.organizations;

/** Запрос противоречит текущему состоянию записи (HTTP 409). */
public class OrganizationConflictException extends RuntimeException {
    public OrganizationConflictException(String message) {
        super(message);
    }
}
