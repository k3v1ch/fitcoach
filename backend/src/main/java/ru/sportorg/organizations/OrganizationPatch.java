package ru.sportorg.organizations;

import com.fasterxml.jackson.annotation.JsonSetter;

public class OrganizationPatch {

    private String name;
    private String description;
    private String address;
    private String timezone;
    private boolean nameProvided;
    private boolean descriptionProvided;
    private boolean addressProvided;
    private boolean timezoneProvided;

    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
        nameProvided = true;
    }

    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        descriptionProvided = true;
    }

    public String getAddress() {
        return address;
    }

    @JsonSetter("address")
    public void setAddress(String address) {
        this.address = address;
        addressProvided = true;
    }

    public String getTimezone() {
        return timezone;
    }

    @JsonSetter("timezone")
    public void setTimezone(String timezone) {
        this.timezone = timezone == null ? null : timezone.trim();
        timezoneProvided = true;
    }

    boolean isNameProvided() {
        return nameProvided;
    }

    boolean isDescriptionProvided() {
        return descriptionProvided;
    }

    boolean isAddressProvided() {
        return addressProvided;
    }

    boolean isTimezoneProvided() {
        return timezoneProvided;
    }
}