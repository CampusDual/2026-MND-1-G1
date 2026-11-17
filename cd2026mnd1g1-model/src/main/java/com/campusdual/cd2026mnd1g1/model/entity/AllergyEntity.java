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
@Table(name = "alergia")
public class AllergyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perro_id", nullable = false)
    private DogEntity dog;

    @Column(name = "descripcion", nullable = false, length = 1000)
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DogEntity getDog() { return dog; }
    public void setDog(DogEntity dog) { this.dog = dog; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
