package ru.sportorg.dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record Dashboard(UUID organizationId, String view, UUID athleteId, Counters counters,
                        Finance finance, List<DashboardTraining> nextTrainings,
                        List<DashboardEvent> nextEvents, ActivityPage recentActivities) {
    public record Counters(Integer athletes, Integer groups, Integer upcomingTrainings, Integer upcomingEvents) { }
    public record Finance(BigDecimal outstandingAmount, BigDecimal overdueAmount) { }
    public record ActivityPage(List<Activity> items, int page, int size, long totalElements, int totalPages) { }
}