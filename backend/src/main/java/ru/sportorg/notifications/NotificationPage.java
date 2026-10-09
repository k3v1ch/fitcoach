package ru.sportorg.notifications;

import java.util.List;

public record NotificationPage(List<Notification> items, int page, int size, long totalElements, int totalPages,
                               long unreadCount) {
}
