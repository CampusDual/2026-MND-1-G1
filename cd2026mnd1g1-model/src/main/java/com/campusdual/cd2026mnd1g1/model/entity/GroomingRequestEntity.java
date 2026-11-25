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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Solicitud de peluquería asociada a un perro y a los servicios solicitados.
 */
@Entity
@Table(name = "solicitud_peluqueria")
public class GroomingRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centro_cliente_id", nullable = false)
    private CenterEntity customerCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perro_id")
    private DogEntity dog;

    @Column(name = "fecha_solicitada", nullable = false)
    private LocalDate requestedDate;

    @Column(name = "hora_solicitada")
    private LocalTime requestedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private GroomingRequestStatus status;

    @Column(name = "notas", length = 2000)
    private String notes;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "solicitud_peluqueria_servicio",
            joinColumns = @JoinColumn(name = "solicitud_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id")
    )
    private Set<GroomingServiceEntity> services = new LinkedHashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CenterEntity getCustomerCenter() { return customerCenter; }
    public void setCustomerCenter(CenterEntity customerCenter) { this.customerCenter = customerCenter; }
    public DogEntity getDog() { return dog; }
    public void setDog(DogEntity dog) { this.dog = dog; }
    public LocalDate getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDate requestedDate) { this.requestedDate = requestedDate; }
    public LocalTime getRequestedTime() { return requestedTime; }
    public void setRequestedTime(LocalTime requestedTime) { this.requestedTime = requestedTime; }
    public GroomingRequestStatus getStatus() { return status; }
    public void setStatus(GroomingRequestStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Set<GroomingServiceEntity> getServices() { return services; }
    public void setServices(Set<GroomingServiceEntity> services) { this.services = services; }
}
