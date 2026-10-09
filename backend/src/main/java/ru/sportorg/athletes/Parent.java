package ru.sportorg.athletes;

import java.util.List;
import java.util.UUID;

/** Родитель организации и его дети в ней. */
record Parent(UUID userId, String fullName, String email, List<Child> athletes) {
    record Child(UUID id, String fullName) {
    }
}
