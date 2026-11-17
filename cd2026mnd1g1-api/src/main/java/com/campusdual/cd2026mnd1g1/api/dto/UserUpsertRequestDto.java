package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public class UserUpsertRequestDto {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 80, message = "El nombre de usuario no puede superar 80 caracteres")
    private String username;

    @NotNull(message = "El centro es obligatorio")
    @Positive(message = "El centro debe ser mayor que cero")
    private Long centerId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    private String firstName;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 160, message = "Los apellidos no pueden superar 160 caracteres")
    private String lastName;

    @NotBlank(message = "El DNI es obligatorio")
    @Size(max = 20, message = "El DNI no puede superar 20 caracteres")
    private String nationalId;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 254, message = "El email no puede superar 254 caracteres")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 120, message = "La contrasena debe tener entre 8 y 120 caracteres")
    private String password;

    @NotNull(message = "El estado enabled es obligatorio")
    private Boolean enabled;

    @NotEmpty(message = "Debe indicarse un rol")
    @Size(min = 1, max = 1, message = "Cada usuario debe tener exactamente un rol")
    private Set<@NotBlank(message = "El nombre del rol no puede estar vacio") String> roles = new HashSet<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getCenterId() { return centerId; }
    public void setCenterId(Long centerId) { this.centerId = centerId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }
}
