package ru.sportorg.trainings;

import java.util.List;

record TrainingPage(List<Training> items, int page, int size, long totalElements, int totalPages) {
}