package com.campusdual.cd2026mnd1g1.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "perro")
public class DogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centro_id", nullable = false)
    private CenterEntity center;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    private TutorEntity tutor;

    @Column(name = "nombre", nullable = false, length = 120)
    private String name;

    @Column(name = "raza", length = 120)
    private String breed;

    @Column(name = "fecha_nacimiento")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tamano", nullable = false, length = 10)
    private DogSize size;

    @Column(name = "temperamento", length = 1000)
    private String temperament;

    @Column(name = "pautas_alimentacion", length = 1000)
    private String feedingInstructions;

    @Column(name = "foto_url", length = 2048)
    private String photoUrl;

    @Column(name = "activo", nullable = false)
    private Boolean active;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CenterEntity getCenter() { return center; }
    public void setCenter(CenterEntity center) { this.center = center; }
    public TutorEntity getTutor() { return tutor; }
    public void setTutor(TutorEntity tutor) { this.tutor = tutor; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public DogSize getSize() { return size; }
    public void setSize(DogSize size) { this.size = size; }
    public String getTemperament() { return temperament; }
    public void setTemperament(String temperament) { this.temperament = temperament; }
    public String getFeedingInstructions() { return feedingInstructions; }
    public void setFeedingInstructions(String feedingInstructions) { this.feedingInstructions = feedingInstructions; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
