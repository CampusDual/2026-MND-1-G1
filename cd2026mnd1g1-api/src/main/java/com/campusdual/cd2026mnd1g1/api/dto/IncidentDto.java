package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDateTime;

public class IncidentDto {

    private Long id;
    private Long reservationId;
    private IncidentSeverityDto severity;
    private String description;
    private LocalDateTime dateTime;

    public IncidentDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public IncidentSeverityDto getSeverity() { return severity; }
    public void setSeverity(IncidentSeverityDto severity) { this.severity = severity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
}
