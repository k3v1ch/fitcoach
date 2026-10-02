package ru.sportorg.documents;

import java.util.List;

record DocumentPage(List<Document> items, int page, int size, long totalElements, int totalPages) {
}