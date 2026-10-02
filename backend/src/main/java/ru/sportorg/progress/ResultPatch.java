package ru.sportorg.progress;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonSetter;

public class ResultPatch {
    private String metricName; private BigDecimal value; private String unit; private LocalDate measuredOn;
    private String comment; private Boolean isPersonalBest; private final java.util.Set<String> provided = new java.util.HashSet<>();
    @JsonSetter("metricName") public void setMetricName(String value) { metricName = value; provided.add("metricName"); }
    @JsonSetter("value") public void setValue(BigDecimal value) { this.value = value; provided.add("value"); }
    @JsonSetter("unit") public void setUnit(String value) { unit = value; provided.add("unit"); }
    @JsonSetter("measuredOn") public void setMeasuredOn(LocalDate value) { measuredOn = value; provided.add("measuredOn"); }
    @JsonSetter("comment") public void setComment(String value) { comment = value; provided.add("comment"); }
    @JsonSetter("isPersonalBest") public void setPersonalBest(Boolean value) { isPersonalBest = value; provided.add("isPersonalBest"); }
    String metricName() { return metricName; } BigDecimal value() { return value; } String unit() { return unit; } LocalDate measuredOn() { return measuredOn; } String comment() { return comment; } Boolean personalBest() { return isPersonalBest; } boolean has(String key) { return provided.contains(key); } boolean empty() { return provided.isEmpty(); }
}