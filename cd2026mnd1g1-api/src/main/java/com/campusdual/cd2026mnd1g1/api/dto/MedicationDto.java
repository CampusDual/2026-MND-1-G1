package com.campusdual.cd2026mnd1g1.api.dto;

public class MedicationDto {

    private Long id;
    private String medication;
    private String dosage;
    private String instructions;

    public MedicationDto() {
    }

    public MedicationDto(Long id, String medication, String dosage, String instructions) {
        this.id = id;
        this.medication = medication;
        this.dosage = dosage;
        this.instructions = instructions;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMedication() { return medication; }
    public void setMedication(String medication) { this.medication = medication; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
