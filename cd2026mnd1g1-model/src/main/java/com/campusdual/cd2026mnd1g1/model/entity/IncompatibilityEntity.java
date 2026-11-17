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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "incompatibilidad", uniqueConstraints = @UniqueConstraint(
        name = "uq_incompatibilidad_perros", columnNames = {"perro_a_id", "perro_b_id"}))
public class IncompatibilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perro_a_id", nullable = false)
    private DogEntity firstDog;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perro_b_id", nullable = false)
    private DogEntity secondDog;

    @Column(name = "motivo", length = 1000)
    private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DogEntity getFirstDog() { return firstDog; }
    public void setFirstDog(DogEntity firstDog) { this.firstDog = firstDog; }
    public DogEntity getSecondDog() { return secondDog; }
    public void setSecondDog(DogEntity secondDog) { this.secondDog = secondDog; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
