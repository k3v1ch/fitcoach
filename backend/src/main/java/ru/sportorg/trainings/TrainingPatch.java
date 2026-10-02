package ru.sportorg.trainings;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSetter;

public class TrainingPatch {
    private String title;
    private UUID groupId;
    private List<UUID> coachIds;
    private UUID venueId;
    private UUID typeId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private List<TrainingStage> plan;
    private String comment;
    private String status;
    private String cancelReason;
    private final java.util.Set<String> provided = new java.util.HashSet<>();
    @JsonSetter("title") public void setTitle(String value) { title = value; provided.add("title"); }
    @JsonSetter("groupId") public void setGroupId(UUID value) { groupId = value; provided.add("groupId"); }
    @JsonSetter("coachIds") public void setCoachIds(List<UUID> value) { coachIds = value; provided.add("coachIds"); }
    @JsonSetter("venueId") public void setVenueId(UUID value) { venueId = value; provided.add("venueId"); }
    @JsonSetter("typeId") public void setTypeId(UUID value) { typeId = value; provided.add("typeId"); }
    @JsonSetter("startsAt") public void setStartsAt(OffsetDateTime value) { startsAt = value; provided.add("startsAt"); }
    @JsonSetter("endsAt") public void setEndsAt(OffsetDateTime value) { endsAt = value; provided.add("endsAt"); }
    @JsonSetter("plan") public void setPlan(List<TrainingStage> value) { plan = value; provided.add("plan"); }
    @JsonSetter("comment") public void setComment(String value) { comment = value; provided.add("comment"); }
    @JsonSetter("status") public void setStatus(String value) { status = value; provided.add("status"); }
    @JsonSetter("cancelReason") public void setCancelReason(String value) { cancelReason = value; provided.add("cancelReason"); }
    String title() { return title; } UUID groupId() { return groupId; } List<UUID> coachIds() { return coachIds; }
    UUID venueId() { return venueId; } UUID typeId() { return typeId; } OffsetDateTime startsAt() { return startsAt; }
    OffsetDateTime endsAt() { return endsAt; } List<TrainingStage> plan() { return plan; } String comment() { return comment; }
    String status() { return status; } String cancelReason() { return cancelReason; } boolean has(String key) { return provided.contains(key); }
    boolean isEmpty() { return provided.isEmpty(); }
}