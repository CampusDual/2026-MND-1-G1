package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.ReceptionReservationDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionReservationPageDto;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionReservationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;

/**
 * Endpoints de consulta de reservas para recepción.
 */
@Validated
@RestController
@RequestMapping("/api/recepcion/reservas")
public class ReceptionReservationController {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final ReceptionReservationService receptionReservationService;

    public ReceptionReservationController(ReceptionReservationService receptionReservationService) {
        this.receptionReservationService = receptionReservationService;
    }

    /**
     * Lista las reservas de una fecha en el centro del usuario autenticado.
     *
     * @param date fecha opcional, por defecto hoy en Europe/Madrid
     * @param page índice de página empezando en cero
     * @param size número de resultados solicitado
     * @param principal usuario autenticado
     * @return página de reservas
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<ReceptionReservationPageDto> getReservations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            Principal principal
    ) {
        return ResponseEntity.ok(this.receptionReservationService.findReservations(
                date,
                page,
                size,
                principal.getName()
        ));
    }

    /**
     * Consulta una reserva del centro del usuario autenticado.
     *
     * @param id identificador de la reserva
     * @param principal usuario autenticado
     * @return detalle de la reserva
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<ReceptionReservationDetailDto> getReservation(
            @PathVariable @Min(0) Long id,
            Principal principal
    ) {
        return ResponseEntity.ok(this.receptionReservationService.findReservation(id, principal.getName()));
    }
}
