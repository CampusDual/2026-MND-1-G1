package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogPageDto;
import com.campusdual.cd2026mnd1g1.api.service.CaregiverPortalService;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDailySummaryDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDailySummaryRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentRequestDto;
import jakarta.validation.Valid;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Locale;
import java.util.List;

/**
 * Consultas privadas de los perros asignados al cuidador para hoy.
 */
@Validated
@RestController
@RequestMapping("/api/cuidador/mis-perros-hoy")
public class CaregiverPortalController {

    private final CaregiverPortalService caregiverPortalService;
    private final String photoDirectory;

    public CaregiverPortalController(
            CaregiverPortalService caregiverPortalService,
            @Value("${app.dogs.photos.directory:}") String photoDirectory
    ) {
        this.caregiverPortalService = caregiverPortalService;
        this.photoDirectory = photoDirectory;
    }

    /**
     * Lista las estancias activas hoy asignadas a los grupos del cuidador.
     */
    @GetMapping
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<CaregiverDogPageDto> getTodayDogs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            Principal principal
    ) {
        return ResponseEntity.ok(this.caregiverPortalService.findTodayDogs(principal.getName(), page, size));
    }

    /**
     * Recupera una ficha solo si la reserva activa está asignada al cuidador.
     */
    @GetMapping("/{reservationId}")
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<CaregiverDogDetailDto> getTodayDog(
            @PathVariable @Min(0) Long reservationId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                this.caregiverPortalService.findTodayDog(reservationId, principal.getName())
        );
    }

    /**
     * Consulta el histórico interno de registros diarios de una estancia asignada.
     */
    @GetMapping("/{reservationId}/registros-diarios")
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<List<DailyRecordDto>> getTodayDailyRecords(
            @PathVariable @Min(0) Long reservationId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                this.caregiverPortalService.findTodayDailyRecords(reservationId, principal.getName())
        );
    }

    /**
     * Añade un registro diario interno a una estancia asignada.
     */
    @PostMapping("/{reservationId}/registros-diarios")
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<DailyRecordDto> createTodayDailyRecord(
            @PathVariable @Min(0) Long reservationId,
            @Valid @RequestBody DailyRecordRequestDto request,
            Principal principal
    ) {
        DailyRecordDto record = this.caregiverPortalService.createTodayDailyRecord(
                reservationId,
                request,
                principal.getName()
        );
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(record);
    }

    /**
     * Consulta el resumen publicado de una estancia asignada.
     */
    @GetMapping("/{reservationId}/resumen")
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<CaregiverDailySummaryDto> getTodaySummary(
            @PathVariable @Min(0) Long reservationId,
            Principal principal
    ) {
        CaregiverDailySummaryDto summary =
                this.caregiverPortalService.findTodaySummary(reservationId, principal.getName());
        return summary == null ? ResponseEntity.ok().build() : ResponseEntity.ok(summary);
    }

    /**
     * Publica una vez el resumen diario de una estancia asignada.
     */
    @PostMapping("/{reservationId}/resumen")
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<CaregiverDailySummaryDto> publishTodaySummary(
            @PathVariable @Min(0) Long reservationId,
            @Valid @RequestBody CaregiverDailySummaryRequestDto request,
            Principal principal
    ) {
        CaregiverDailySummaryDto summary =
                this.caregiverPortalService.publishTodaySummary(
                        reservationId,
                        request,
                        principal.getName()
                );
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(summary);
    }

    /**
     * Consulta las incidencias internas de una estancia asignada.
     */
    @GetMapping("/{reservationId}/incidencias")
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<List<IncidentDto>> getTodayIncidents(
            @PathVariable @Min(0) Long reservationId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                this.caregiverPortalService.findTodayIncidents(reservationId, principal.getName())
        );
    }

    /**
     * Registra una incidencia interna en una estancia asignada.
     */
    @PostMapping("/{reservationId}/incidencias")
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<IncidentDto> createTodayIncident(
            @PathVariable @Min(0) Long reservationId,
            @Valid @RequestBody IncidentRequestDto request,
            Principal principal
    ) {
        IncidentDto incident =
                this.caregiverPortalService.createTodayIncident(
                        reservationId,
                        request,
                        principal.getName()
                );
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(incident);
    }

    /**
     * Sirve una foto privada solo si la reserva pertenece al cuidador autenticado.
     */
    @GetMapping("/{reservationId}/foto")
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<Resource> getTodayDogPhoto(
            @PathVariable @Min(0) Long reservationId,
            Principal principal
    ) {
        String photoReference = this.caregiverPortalService.findTodayDogPhotoReference(
                reservationId,
                principal.getName()
        );
        Path photo = resolvePhoto(photoReference);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(photo))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"dog-photo\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(photo));
    }

    private Path resolvePhoto(String photoReference) {
        if (this.photoDirectory == null
                || this.photoDirectory.isBlank()
                || photoReference == null
                || !photoReference.matches("(?i)[a-z0-9][a-z0-9._-]{0,127}\\.(jpg|jpeg|png|gif|webp)")) {
            throw new ResourceNotFoundException("No hay una foto disponible para este perro");
        }
        try {
            Path root = Path.of(this.photoDirectory).toRealPath();
            Path candidate = root.resolve(photoReference).normalize();
            if (!candidate.startsWith(root)) {
                throw new ResourceNotFoundException("No hay una foto disponible para este perro");
            }
            Path realPhoto = candidate.toRealPath();
            if (!realPhoto.startsWith(root) || !Files.isRegularFile(realPhoto)) {
                throw new ResourceNotFoundException("No hay una foto disponible para este perro");
            }
            return realPhoto;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResourceNotFoundException("No hay una foto disponible para este perro");
        }
    }

    private MediaType resolveMediaType(Path photo) {
        String extension = photo.getFileName().toString();
        int dot = extension.lastIndexOf('.');
        String normalized = dot >= 0
                ? extension.substring(dot).toLowerCase(Locale.ROOT)
                : "";
        return switch (normalized) {
            case ".jpg", ".jpeg" -> MediaType.IMAGE_JPEG;
            case ".png" -> MediaType.IMAGE_PNG;
            case ".gif" -> MediaType.IMAGE_GIF;
            case ".webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
