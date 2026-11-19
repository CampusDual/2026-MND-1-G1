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

import java.time.LocalDateTime;

@Entity
@Table(name = "check_registro")
public class CheckRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private ReservationEntity reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recogido_por_tutor_id")
    private TutorEntity collectedByTutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recogido_por_persona_autorizada_id")
    private AuthorizedPersonEntity collectedByAuthorizedPerson;

    @Column(name = "recogido_nombre", length = 281)
    private String collectorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private CheckRecordType type;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "estado_mascota", length = 500)
    private String petCondition;

    @Column(name = "observaciones", length = 2000)
    private String observations;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ReservationEntity getReservation() { return reservation; }
    public void setReservation(ReservationEntity reservation) { this.reservation = reservation; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public TutorEntity getCollectedByTutor() { return collectedByTutor; }
    public void setCollectedByTutor(TutorEntity collectedByTutor) { this.collectedByTutor = collectedByTutor; }
    public AuthorizedPersonEntity getCollectedByAuthorizedPerson() { return collectedByAuthorizedPerson; }
    public void setCollectedByAuthorizedPerson(AuthorizedPersonEntity collectedByAuthorizedPerson) {
        this.collectedByAuthorizedPerson = collectedByAuthorizedPerson;
    }
    public String getCollectorName() { return collectorName; }
    public void setCollectorName(String collectorName) { this.collectorName = collectorName; }
    public CheckRecordType getType() { return type; }
    public void setType(CheckRecordType type) { this.type = type; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getPetCondition() { return petCondition; }
    public void setPetCondition(String petCondition) { this.petCondition = petCondition; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
