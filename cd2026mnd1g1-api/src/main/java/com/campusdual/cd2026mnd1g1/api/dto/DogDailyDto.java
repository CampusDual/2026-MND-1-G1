package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DogDailyDto {

    private Long id;
    private String name;
    private String breed;
    private String size;
    private LocalDate birthDate;
    private List<AllergyDto> allergies = new ArrayList<>();
    private List<MedicationDto> medications = new ArrayList<>();
    private List<IncompatibilityDto> incompatibilities = new ArrayList<>();

    public DogDailyDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public List<AllergyDto> getAllergies() { return allergies; }
    public void setAllergies(List<AllergyDto> allergies) { this.allergies = allergies; }
    public List<MedicationDto> getMedications() { return medications; }
    public void setMedications(List<MedicationDto> medications) { this.medications = medications; }
    public List<IncompatibilityDto> getIncompatibilities() { return incompatibilities; }
    public void setIncompatibilities(List<IncompatibilityDto> incompatibilities) { this.incompatibilities = incompatibilities; }
}
