package ru.sportorg.finance;

import java.util.List;

record ChargePage(List<Charge> items, int page, int size, long totalElements, int totalPages) {
}
