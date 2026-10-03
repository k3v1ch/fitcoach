package ru.sportorg.trainings;

import java.math.BigDecimal;
import java.time.LocalDate;

record AttendanceSummary(LocalDate from, LocalDate to, int trainingCount, int participantRecords, int present,
                         int sick, int absent, int unmarked, BigDecimal attendancePercent) {
}
