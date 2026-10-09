package ru.sportorg.finance;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** Изменение или отмена начисления: непереданное поле не меняется, переданное null — очищается (comment). */
public class ChargePatch {

    private String title;
    private BigDecimal amount;
    private LocalDate dueOn;
    private String comment;
    private String status;
    private String cancelReason;
    private final Set<String> provided = new HashSet<>();

    @JsonSetter("title") public void setTitle(String value) { title = value; provided.add("title"); }
    @JsonSetter("amount") public void setAmount(BigDecimal value) { amount = value; provided.add("amount"); }
    @JsonSetter("dueOn") public void setDueOn(LocalDate value) { dueOn = value; provided.add("dueOn"); }
    @JsonSetter("comment") public void setComment(String value) { comment = value; provided.add("comment"); }
    @JsonSetter("status") public void setStatus(String value) { status = value; provided.add("status"); }
    @JsonSetter("cancelReason") public void setCancelReason(String value) { cancelReason = value; provided.add("cancelReason"); }

    String title() { return title; }
    BigDecimal amount() { return amount; }
    LocalDate dueOn() { return dueOn; }
    String comment() { return comment; }
    String status() { return status; }
    String cancelReason() { return cancelReason; }

    boolean has(String field) { return provided.contains(field); }
    boolean isEmpty() { return provided.isEmpty(); }
}
