package ru.sportorg.documents;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** Исправление названия и сроков документа: непереданное поле не меняется, null очищает дату. */
public class DocumentPatch {

    private String title;
    private LocalDate issuedOn;
    private LocalDate validUntil;
    private final Set<String> provided = new HashSet<>();

    @JsonSetter("title") public void setTitle(String value) { title = value; provided.add("title"); }
    @JsonSetter("issuedOn") public void setIssuedOn(LocalDate value) { issuedOn = value; provided.add("issuedOn"); }
    @JsonSetter("validUntil") public void setValidUntil(LocalDate value) { validUntil = value; provided.add("validUntil"); }

    String title() { return title; }
    LocalDate issuedOn() { return issuedOn; }
    LocalDate validUntil() { return validUntil; }

    boolean has(String field) { return provided.contains(field); }
    boolean isEmpty() { return provided.isEmpty(); }
}
