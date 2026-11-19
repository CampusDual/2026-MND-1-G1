package com.campusdual.cd2026mnd1g1.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Registra el resultado de una solicitud de alta para hacer seguros los reintentos.
 */
@Entity
@Table(name = "tutor_dog_submission", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tutor_dog_submission_key", columnNames = {"tutor_id", "idempotency_key"}),
        @UniqueConstraint(name = "uq_tutor_dog_submission_dog", columnNames = "dog_id")
})
public class TutorDogSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorEntity tutor;

    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dog_id", nullable = false, unique = true)
    private DogEntity dog;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TutorEntity getTutor() { return tutor; }
    public void setTutor(TutorEntity tutor) { this.tutor = tutor; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public DogEntity getDog() { return dog; }
    public void setDog(DogEntity dog) { this.dog = dog; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
