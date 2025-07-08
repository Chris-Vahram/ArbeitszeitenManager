package org.example.arbeitszeitenmanager.model;

import javafx.beans.property.*;
import java.time.*;

public class WorkDay {
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> start = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> end_time = new SimpleObjectProperty<>();
    private final StringProperty notes = new SimpleStringProperty();

    public String getNotes() {
        return notes.get();
    }

    public void  setNotes(String notes) {
        this.notes.set(notes);
    }

    public StringProperty notesProperty() {
        return notes;
    }

    // dynamisch anpassbar
    private final ObjectProperty<Duration> restzeitHeute = new SimpleObjectProperty<>(Duration.ZERO);

    public WorkDay(LocalDate date) {
        this.date.set(date);
    }

    public LocalDate getDate() {
        return date.get();
    }

    public LocalTime getStart() {
        return start.get();
    }

    public void setStart(LocalTime start) {
        this.start.set(start);
    }

    public LocalTime getEnd_time() {
        return end_time.get();
    }

    public void setEnd_time(LocalTime end_time) {
        this.end_time.set(end_time);
    }

    public Duration getIstZeit() {
        if (start.get() != null && end_time.get() != null && !end_time.get().isBefore(start.get())) {
            return Duration.between(start.get(), end_time.get());
        } else {
            return Duration.ZERO;
        }
    }

    public Duration getSollZeit() {
        if (date.get().getDayOfWeek() != DayOfWeek.SATURDAY && date.get().getDayOfWeek() != DayOfWeek.SUNDAY) {
            return Duration.ofMinutes(Math.round(8.2 * 60)); // ergibt 492 Minuten = 8h 12min
        } else {
            return Duration.ZERO;
        }
    }

    public Duration getRestzeitHeute() {
        return restzeitHeute.get();
    }

    public void setRestzeitHeute(Duration d) {
        this.restzeitHeute.set(d);
    }
}
