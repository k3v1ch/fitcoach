package ru.sportorg.events;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;

public class EventPatch {
    private String title; private String description; private OffsetDateTime startsAt; private OffsetDateTime endsAt;
    private String location; private java.math.BigDecimal costPerAthlete; private java.math.BigDecimal targetAmount;
    private LocalDate collectionDueOn; private OffsetDateTime responseDeadline; private List<String> requiredDocumentTypes;
    private String status; private final java.util.Set<String> provided = new java.util.HashSet<>();
    @JsonSetter("title") public void setTitle(String value) { title = value; provided.add("title"); }
    @JsonSetter("description") public void setDescription(String value) { description = value; provided.add("description"); }
    @JsonSetter("startsAt") public void setStartsAt(OffsetDateTime value) { startsAt = value; provided.add("startsAt"); }
    @JsonSetter("endsAt") public void setEndsAt(OffsetDateTime value) { endsAt = value; provided.add("endsAt"); }
    @JsonSetter("location") public void setLocation(String value) { location = value; provided.add("location"); }
    @JsonSetter("costPerAthlete") public void setCostPerAthlete(java.math.BigDecimal value) { costPerAthlete = value; provided.add("costPerAthlete"); }
    @JsonSetter("targetAmount") public void setTargetAmount(java.math.BigDecimal value) { targetAmount = value; provided.add("targetAmount"); }
    @JsonSetter("collectionDueOn") public void setCollectionDueOn(LocalDate value) { collectionDueOn = value; provided.add("collectionDueOn"); }
    @JsonSetter("responseDeadline") public void setResponseDeadline(OffsetDateTime value) { responseDeadline = value; provided.add("responseDeadline"); }
    @JsonSetter("requiredDocumentTypes") public void setRequiredDocumentTypes(List<String> value) { requiredDocumentTypes = value; provided.add("requiredDocumentTypes"); }
    @JsonSetter("status") public void setStatus(String value) { status = value; provided.add("status"); }
    String title() { return title; } String description() { return description; } OffsetDateTime startsAt() { return startsAt; } OffsetDateTime endsAt() { return endsAt; } String location() { return location; } java.math.BigDecimal costPerAthlete() { return costPerAthlete; } java.math.BigDecimal targetAmount() { return targetAmount; } LocalDate collectionDueOn() { return collectionDueOn; } OffsetDateTime responseDeadline() { return responseDeadline; } List<String> requiredDocumentTypes() { return requiredDocumentTypes; } String status() { return status; } boolean has(String key) { return provided.contains(key); } boolean empty() { return provided.isEmpty(); }
}