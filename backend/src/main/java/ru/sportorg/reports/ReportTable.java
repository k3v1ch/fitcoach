package ru.sportorg.reports;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ReportTable(ReportType type, Instant generatedAt, List<String> columns,
                          List<Map<String, Object>> rows, int page, int size,
                          long totalElements, int totalPages) {
}