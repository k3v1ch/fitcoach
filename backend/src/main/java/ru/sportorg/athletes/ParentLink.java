package ru.sportorg.athletes;

import java.util.UUID;

record ParentLink(UUID parentUserId, String fullName, String email, String relationship) {

    ParentLink(UUID parentUserId, String fullName, String email, String phone, String relationship) {
        this(parentUserId, fullName, email, relationship);
    }
}