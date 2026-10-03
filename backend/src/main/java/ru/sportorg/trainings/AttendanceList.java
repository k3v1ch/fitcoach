package ru.sportorg.trainings;

import java.util.List;

record AttendanceList(List<AttendanceEntry> items, int page, int size, long totalElements, int totalPages,
                      AttendanceSummary summary) {
}
