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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reserva", uniqueConstraints = @UniqueConstraint(
        name = "uq_reserva_perro_fecha", columnNames = {"perro_id", "fecha"}))
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perro_id", nullable = false)
    private DogEntity dog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bono_id")
    private VoucherEntity voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private DaycareGroupEntity group;

    @Column(name = "fecha", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false, length = 20)
    private ReservationModality modality;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DogEntity getDog() { return dog; }
    public void setDog(DogEntity dog) { this.dog = dog; }
    public VoucherEntity getVoucher() { return voucher; }
    public void setVoucher(VoucherEntity voucher) { this.voucher = voucher; }
    public DaycareGroupEntity getGroup() { return group; }
    public void setGroup(DaycareGroupEntity group) { this.group = group; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public ReservationModality getModality() { return modality; }
    public void setModality(ReservationModality modality) { this.modality = modality; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
