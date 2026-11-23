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
@Table(name = "grupo", uniqueConstraints = @UniqueConstraint(
        name = "uq_grupo_centro_nombre", columnNames = {"centro_id", "nombre"}))
public class DaycareGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centro_id", nullable = false)
    private CenterEntity center;

    @Column(name = "nombre", nullable = false, length = 120)
    private String name;

    @Column(name = "sala", length = 120)
    private String room;

    @Column(name = "capacidad_max", nullable = false)
    private Integer maxCapacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuidador_id")
    private UserEntity caregiver;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CenterEntity getCenter() { return center; }
    public void setCenter(CenterEntity center) { this.center = center; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
    public UserEntity getCaregiver() { return caregiver; }
    public void setCaregiver(UserEntity caregiver) { this.caregiver = caregiver; }
}
