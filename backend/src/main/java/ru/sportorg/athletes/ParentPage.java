package ru.sportorg.athletes;

import java.util.List;

record ParentPage(List<Parent> items, int page, int size, long totalElements, int totalPages) {
}
