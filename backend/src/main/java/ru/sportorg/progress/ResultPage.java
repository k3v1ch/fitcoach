package ru.sportorg.progress;

import java.util.List;

record ResultPage(List<Result> items, int page, int size, long totalElements, int totalPages) {
}