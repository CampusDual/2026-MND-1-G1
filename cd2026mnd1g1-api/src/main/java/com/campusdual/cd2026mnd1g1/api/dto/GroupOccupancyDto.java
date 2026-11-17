package com.campusdual.cd2026mnd1g1.api.dto;

public class GroupOccupancyDto {

    private Long groupId;
    private long occupied;
    private int capacityMax;

    public GroupOccupancyDto() {
    }

    public GroupOccupancyDto(Long groupId, long occupied, int capacityMax) {
        this.groupId = groupId;
        this.occupied = occupied;
        this.capacityMax = capacityMax;
    }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public long getOccupied() { return occupied; }
    public void setOccupied(long occupied) { this.occupied = occupied; }
    public int getCapacityMax() { return capacityMax; }
    public void setCapacityMax(int capacityMax) { this.capacityMax = capacityMax; }
}
