package ru.sportorg.dictionaries;

import java.time.Instant;
import java.util.UUID;

record DictionaryItem(UUID id, UUID organizationId, String dictionaryType, String name, String description,
                      String address, int sortOrder, String status, Instant createdAt, Instant updatedAt) {
}