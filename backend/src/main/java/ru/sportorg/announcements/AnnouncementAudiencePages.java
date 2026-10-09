package ru.sportorg.announcements;

import java.util.List;

final class AnnouncementAudiencePages {
    private AnnouncementAudiencePages() {
    }

    record RecipientPage(List<AnnouncementRecipient> items, int page, int size, long totalElements, int totalPages) {
    }

    record ResponsePage(List<AnnouncementResponseEntry> items, int page, int size, long totalElements, int totalPages) {
    }
}
