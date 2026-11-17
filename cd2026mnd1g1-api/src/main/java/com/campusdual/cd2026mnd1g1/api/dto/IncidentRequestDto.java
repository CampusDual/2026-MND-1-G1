package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class IncidentRequestDto {

    @NotNull(message = "La gravedad es obligatoria")
    private IncidentSeverityDto severity;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    private String description;

    public IncidentSeverityDto getSeverity() { return severity; }
    public void setSeverity(IncidentSeverityDto severity) { this.severity = severity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
