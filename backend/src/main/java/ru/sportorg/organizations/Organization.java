package ru.sportorg.organizations;

import java.time.Instant;
import java.util.UUID;

public record Organization(UUID id, String name, String description, String address, String timezone,
                           Instant createdAt, Instant updatedAt) {
}