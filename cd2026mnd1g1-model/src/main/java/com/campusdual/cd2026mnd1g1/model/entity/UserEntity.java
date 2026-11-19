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
import jakarta.persistence.OneToOne;

import java.time.LocalDateTime;

@Entity
@Table(name = "USUARIOS")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centro_id", nullable = false)
    private CenterEntity center;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private RoleEntity role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", unique = true)
    private TutorEntity tutor;

    @Column(name = "nombre", nullable = false, length = 120)
    private String firstName;

    @Column(name = "apellidos", nullable = false, length = 160)
    private String lastName;

    @Column(name = "dni", nullable = false, unique = true, length = 20)
    private String nationalId;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "username", nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

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
    public RoleEntity getRole() { return role; }
    public void setRole(RoleEntity role) { this.role = role; }
    public TutorEntity getTutor() { return tutor; }
    public void setTutor(TutorEntity tutor) { this.tutor = tutor; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
