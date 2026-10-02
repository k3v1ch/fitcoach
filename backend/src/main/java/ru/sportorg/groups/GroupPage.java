package ru.sportorg.groups;

import java.util.List;

record GroupPage(List<Group> items, int page, int size, long totalElements, int totalPages) {
}