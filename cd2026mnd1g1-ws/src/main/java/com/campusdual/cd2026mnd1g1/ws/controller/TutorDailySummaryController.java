package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.TutorDailySummaryDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDailySummaryDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorNotificationCountDto;
import com.campusdual.cd2026mnd1g1.api.service.TutorPortalService;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Consultas de resúmenes publicados y contador interno del tutor.
 */
@Validated
@RestController
@RequestMapping("/api/tutor")
public class TutorDailySummaryController {

    private final TutorPortalService tutorPortalService;

    public TutorDailySummaryController(TutorPortalService tutorPortalService) {
        this.tutorPortalService = tutorPortalService;
    }

    /**
     * Lista resúmenes publicados únicamente para un perro propio.
     *
     * @param dogId identificador del perro
     * @param principal identidad autenticada
     * @return resúmenes ordenados de más reciente a más antiguo
     */
    @GetMapping("/mis-perros/{dogId}/resumenes")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<List<TutorDailySummaryDto>> getMyDogSummaries(
            @PathVariable @Min(0) Long dogId,
            Principal principal
    ) {
        return ResponseEntity.ok(this.tutorPortalService.findMyDailySummaries(dogId, principal.getName()));
    }

    /**
     * Recupera un resumen publicado del perro propio indicado.
     *
     * @param dogId identificador del perro
     * @param summaryId identificador del resumen
     * @param principal identidad autenticada
     * @return detalle del resumen publicado
     */
    @GetMapping("/mis-perros/{dogId}/resumenes/{summaryId}")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<TutorDailySummaryDetailDto> getMyDogSummary(
            @PathVariable @Min(0) Long dogId,
            @PathVariable @Min(0) Long summaryId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                this.tutorPortalService.findMyDailySummary(dogId, summaryId, principal.getName())
        );
    }

    /**
     * Marca como visto un resumen publicado del perro propio.
     *
     * @param dogId identificador del perro
     * @param summaryId identificador del resumen
     * @param principal identidad autenticada
     * @return respuesta sin contenido
     */
    @PostMapping("/mis-perros/{dogId}/resumenes/{summaryId}/lectura")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<Void> markMyDogSummaryAsViewed(
            @PathVariable @Min(0) Long dogId,
            @PathVariable @Min(0) Long summaryId,
            Principal principal
    ) {
        this.tutorPortalService.markMyDailySummaryAsViewed(dogId, summaryId, principal.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Devuelve el contador de resúmenes publicados que el tutor no ha visto.
     *
     * @param principal identidad autenticada
     * @return contador calculado por el servidor
     */
    @GetMapping("/notificaciones/contador")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<TutorNotificationCountDto> getMyNotificationCount(Principal principal) {
        return ResponseEntity.ok(this.tutorPortalService.getMyNotificationCount(principal.getName()));
    }
}
