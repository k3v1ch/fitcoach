package ru.sportorg.athletes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSetter;

public class AthletePatch {

    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate birthDate;
    private UUID userId;
    private String status;
    private LocalDate enrolledOn;
    private String note;
    private List<ParentLinkWrite> parentLinks;
    private final java.util.Set<String> provided = new java.util.HashSet<>();

    @JsonSetter("firstName") public void setFirstName(String value) { firstName = value; provided.add("firstName"); }
    @JsonSetter("lastName") public void setLastName(String value) { lastName = value; provided.add("lastName"); }
    @JsonSetter("middleName") public void setMiddleName(String value) { middleName = value; provided.add("middleName"); }
    @JsonSetter("birthDate") public void setBirthDate(LocalDate value) { birthDate = value; provided.add("birthDate"); }
    @JsonSetter("userId") public void setUserId(UUID value) { userId = value; provided.add("userId"); }
    @JsonSetter("status") public void setStatus(String value) { status = value; provided.add("status"); }
    @JsonSetter("enrolledOn") public void setEnrolledOn(LocalDate value) { enrolledOn = value; provided.add("enrolledOn"); }
    @JsonSetter("note") public void setNote(String value) { note = value; provided.add("note"); }
    @JsonSetter("parentLinks") public void setParentLinks(List<ParentLinkWrite> value) { parentLinks = value; provided.add("parentLinks"); }

    String firstName() { return firstName; }
    String lastName() { return lastName; }
    String middleName() { return middleName; }
    LocalDate birthDate() { return birthDate; }
    UUID userId() { return userId; }
    String status() { return status; }
    LocalDate enrolledOn() { return enrolledOn; }
    String note() { return note; }
    List<ParentLinkWrite> parentLinks() { return parentLinks; }
    boolean has(String field) { return provided.contains(field); }
    boolean isEmpty() { return provided.isEmpty(); }
}