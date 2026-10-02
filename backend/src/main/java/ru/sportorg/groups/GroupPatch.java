package ru.sportorg.groups;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSetter;

public class GroupPatch {
    private String name;
    private List<UUID> coachIds;
    private String description;
    private String status;
    private final java.util.Set<String> provided = new java.util.HashSet<>();

    @JsonSetter("name") public void setName(String value) { name = value; provided.add("name"); }
    @JsonSetter("coachIds") public void setCoachIds(List<UUID> value) { coachIds = value; provided.add("coachIds"); }
    @JsonSetter("description") public void setDescription(String value) { description = value; provided.add("description"); }
    @JsonSetter("status") public void setStatus(String value) { status = value; provided.add("status"); }
    String name() { return name; }
    List<UUID> coachIds() { return coachIds; }
    String description() { return description; }
    String status() { return status; }
    boolean has(String field) { return provided.contains(field); }
    boolean isEmpty() { return provided.isEmpty(); }
}