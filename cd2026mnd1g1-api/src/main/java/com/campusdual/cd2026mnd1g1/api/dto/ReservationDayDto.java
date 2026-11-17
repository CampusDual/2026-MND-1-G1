package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;

public class ReservationDayDto {

    private Long id;
    private LocalDate date;
    private String modality;
    private String status;
    private DogDailyDto dog;
    private GroupOccupancyDto groupOccupancy;

    public ReservationDayDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public DogDailyDto getDog() { return dog; }
    public void setDog(DogDailyDto dog) { this.dog = dog; }
    public GroupOccupancyDto getGroupOccupancy() { return groupOccupancy; }
    public void setGroupOccupancy(GroupOccupancyDto groupOccupancy) { this.groupOccupancy = groupOccupancy; }
}
