package ru.sportorg.documents;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

record StoredFile(UUID id, UUID organizationId, String originalName, String contentType,
                  long sizeBytes, @JsonIgnore String storageName, UUID uploadedBy, Instant uploadedAt) {
}