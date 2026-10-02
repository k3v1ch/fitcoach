package ru.sportorg.athletes;

import java.util.List;

record AthletePage(List<Athlete> items, int page, int size, long totalElements, int totalPages) {
}