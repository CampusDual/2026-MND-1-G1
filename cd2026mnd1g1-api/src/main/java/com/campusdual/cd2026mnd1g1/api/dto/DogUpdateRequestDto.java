package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Campos básicos permitidos al actualizar un perro.
 */
public class DogUpdateRequestDto {

    private String name;
    private String breed;
    private String size;
    private LocalDate birthDate;
    private boolean clearBreed;
    private boolean clearBirthDate;
    private boolean confirmMatches;

    public DogUpdateRequestDto() {
    }

    @NotBlank(message = "El nombre del perro es obligatorio")
    @Size(max = 120, message = "El nombre del perro no puede superar 120 caracteres")
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Size(max = 120, message = "La raza no puede superar 120 caracteres")
    public String getBreed() {
        return this.breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    @NotBlank(message = "El tamaño es obligatorio")
    @Pattern(regexp = "PEQUENO|MEDIANO|GRANDE", message = "El tamaño no es válido")
    public String getSize() {
        return this.size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public LocalDate getBirthDate() {
        return this.birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public boolean isClearBreed() {
        return this.clearBreed;
    }

    public void setClearBreed(boolean clearBreed) {
        this.clearBreed = clearBreed;
    }

    public boolean isClearBirthDate() {
        return this.clearBirthDate;
    }

    public void setClearBirthDate(boolean clearBirthDate) {
        this.clearBirthDate = clearBirthDate;
    }

    public boolean isConfirmMatches() {
        return this.confirmMatches;
    }

    public void setConfirmMatches(boolean confirmMatches) {
        this.confirmMatches = confirmMatches;
    }
}
