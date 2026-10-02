package ru.sportorg.events;

import java.util.List;

record EventPage(List<Event> items, int page, int size, long totalElements, int totalPages) {
}