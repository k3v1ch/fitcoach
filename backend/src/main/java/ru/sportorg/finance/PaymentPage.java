package ru.sportorg.finance;

import java.util.List;

record PaymentPage(List<Payment> items, int page, int size, long totalElements, int totalPages) {
}