package ru.sportorg.groups;

import java.util.List;

record SectionPage(List<Section> items, int page, int size, long totalElements, int totalPages) {
}