package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DailyRecordRequestDto {

    @NotNull(message = "El tipo de actividad es obligatorio")
    private DailyRecordTypeDto type;

    @Size(max = 2000, message = "Las notas no pueden superar 2000 caracteres")
    private String notes;

    public DailyRecordTypeDto getType() { return type; }
    public void setType(DailyRecordTypeDto type) { this.type = type; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
