package ru.sportorg.trainings;

import java.util.List;
import java.util.UUID;

/** Тренировка, её отчёт и посещаемость (6.8, №029); раздел без права на него — null. */
record TrainingDetail(UUID trainingId, Training training, TrainingReport report, List<Attendance> attendance) {
}
