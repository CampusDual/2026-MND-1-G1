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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Cita de peluquería programada para un perro.
 */
@Entity
@Table(name = "cita_peluqueria")
public class GroomingAppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perro_id", nullable = false)
    private DogEntity dog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peluquero_id")
    private UserEntity groomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_usuario_id")
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id")
    private GroomingRequestEntity request;

    @Column(name = "inicio", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "duracion_min", nullable = false)
    private Integer durationMinutes;

    @Column(name = "lugar", length = 20)
    private String location;

    @Column(name = "observaciones", length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private GroomingAppointmentStatus status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "cita_servicio",
            joinColumns = @JoinColumn(name = "cita_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id")
    )
    private Set<GroomingServiceEntity> services = new LinkedHashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DogEntity getDog() { return dog; }
    public void setDog(DogEntity dog) { this.dog = dog; }
    public UserEntity getGroomer() { return groomer; }
    public void setGroomer(UserEntity groomer) { this.groomer = groomer; }
    public UserEntity getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserEntity createdBy) { this.createdBy = createdBy; }
    public GroomingRequestEntity getRequest() { return request; }
    public void setRequest(GroomingRequestEntity request) { this.request = request; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public GroomingAppointmentStatus getStatus() { return status; }
    public void setStatus(GroomingAppointmentStatus status) { this.status = status; }
    public Set<GroomingServiceEntity> getServices() { return services; }
    public void setServices(Set<GroomingServiceEntity> services) { this.services = services; }
}
