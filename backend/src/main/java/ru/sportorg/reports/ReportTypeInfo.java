package ru.sportorg.reports;

import java.util.List;

public record ReportTypeInfo(ReportType type, String name, List<String> formats) {
}