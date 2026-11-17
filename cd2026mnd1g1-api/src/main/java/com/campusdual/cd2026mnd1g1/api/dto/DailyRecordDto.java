package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDateTime;

public class DailyRecordDto {

    private Long id;
    private Long reservationId;
    private DailyRecordTypeDto type;
    private LocalDateTime dateTime;
    private String notes;

    public DailyRecordDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public DailyRecordTypeDto getType() { return type; }
    public void setType(DailyRecordTypeDto type) { this.type = type; }
    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
