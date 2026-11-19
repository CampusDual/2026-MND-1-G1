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
 * Persona registrada por un tutor como autorizada para recoger sus perros.
 */
@Entity
@Table(name = "persona_autorizada")
public class AuthorizedPersonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorEntity tutor;

    @Column(name = "nombre", nullable = false, length = 120)
    private String name;

    @Column(name = "telefono", nullable = false, length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_usuario_id")
    private UserEntity createdBy;

    @Column(name = "creado_en")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TutorEntity getTutor() { return tutor; }
    public void setTutor(TutorEntity tutor) { this.tutor = tutor; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public UserEntity getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserEntity createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
