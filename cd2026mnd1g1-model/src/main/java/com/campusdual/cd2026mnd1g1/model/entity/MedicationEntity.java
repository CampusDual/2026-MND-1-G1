package com.campusdual.cd2026mnd1g1.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "medicacion")
public class MedicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perro_id", nullable = false)
    private DogEntity dog;

    @Column(name = "medicamento", nullable = false, length = 160)
    private String medication;

    @Column(name = "dosis", length = 255)
    private String dosage;

    @Column(name = "pauta", nullable = false, length = 1000)
    private String instructions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DogEntity getDog() { return dog; }
    public void setDog(DogEntity dog) { this.dog = dog; }
    public String getMedication() { return medication; }
    public void setMedication(String medication) { this.medication = medication; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
