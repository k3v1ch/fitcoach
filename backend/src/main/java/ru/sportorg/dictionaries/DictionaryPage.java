package ru.sportorg.dictionaries;

import java.util.List;

record DictionaryPage(List<DictionaryItem> items, int page, int size, long totalElements, int totalPages) {
}