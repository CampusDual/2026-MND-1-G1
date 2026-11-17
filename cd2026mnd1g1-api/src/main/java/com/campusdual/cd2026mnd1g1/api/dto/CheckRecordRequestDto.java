package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CheckRecordRequestDto {

    @NotNull(message = "El tipo de registro es obligatorio")
    private CheckRecordTypeDto type;

    @Size(max = 500, message = "El estado de la mascota no puede superar 500 caracteres")
    private String petCondition;

    @Size(max = 2000, message = "Las observaciones no pueden superar 2000 caracteres")
    private String observations;

    public CheckRecordTypeDto getType() { return type; }
    public void setType(CheckRecordTypeDto type) { this.type = type; }
    public String getPetCondition() { return petCondition; }
    public void setPetCondition(String petCondition) { this.petCondition = petCondition; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
