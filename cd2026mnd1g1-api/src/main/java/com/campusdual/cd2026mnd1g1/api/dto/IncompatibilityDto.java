package com.campusdual.cd2026mnd1g1.api.dto;

public class IncompatibilityDto {

    private Long id;
    private Long incompatibleDogId;
    private String incompatibleDogName;
    private String reason;

    public IncompatibilityDto() {
    }

    public IncompatibilityDto(Long id, Long incompatibleDogId, String incompatibleDogName, String reason) {
        this.id = id;
        this.incompatibleDogId = incompatibleDogId;
        this.incompatibleDogName = incompatibleDogName;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIncompatibleDogId() { return incompatibleDogId; }
    public void setIncompatibleDogId(Long incompatibleDogId) { this.incompatibleDogId = incompatibleDogId; }
    public String getIncompatibleDogName() { return incompatibleDogName; }
    public void setIncompatibleDogName(String incompatibleDogName) { this.incompatibleDogName = incompatibleDogName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
