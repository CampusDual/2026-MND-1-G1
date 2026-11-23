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

import java.time.LocalDateTime;

/**
 * Resumen diario publicado para consulta posterior del tutor.
 */
@Entity
@Table(name = "resumen_diario")
public class DailySummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private ReservationEntity reservation;

    @Column(name = "destinatario", nullable = false, length = 10)
    private String recipient;

    @Column(name = "texto_ia", nullable = false, length = 4000)
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuidador_id")
    private UserEntity caregiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id")
    private CenterEntity center;

    @Column(name = "estado", length = 20)
    private String status;

    @Column(name = "generado_en", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "publicado_en")
    private LocalDateTime publishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ReservationEntity getReservation() { return reservation; }
    public void setReservation(ReservationEntity reservation) { this.reservation = reservation; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public UserEntity getCaregiver() { return caregiver; }
    public void setCaregiver(UserEntity caregiver) { this.caregiver = caregiver; }
    public CenterEntity getCenter() { return center; }
    public void setCenter(CenterEntity center) { this.center = center; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
