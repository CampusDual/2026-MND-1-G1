package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDateTime;

public class CheckRecordDto {

    private Long id;
    private Long reservationId;
    private CheckRecordTypeDto type;
    private LocalDateTime dateTime;
    private String petCondition;
    private String observations;
    private String reservationStatus;
    private String collectorType;
    private String collectorName;

    public CheckRecordDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public CheckRecordTypeDto getType() { return type; }
    public void setType(CheckRecordTypeDto type) { this.type = type; }
    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public String getPetCondition() { return petCondition; }
    public void setPetCondition(String petCondition) { this.petCondition = petCondition; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public String getReservationStatus() { return reservationStatus; }
    public void setReservationStatus(String reservationStatus) { this.reservationStatus = reservationStatus; }
    public String getCollectorType() { return collectorType; }
    public void setCollectorType(String collectorType) { this.collectorType = collectorType; }
    public String getCollectorName() { return collectorName; }
    public void setCollectorName(String collectorName) { this.collectorName = collectorName; }
}
