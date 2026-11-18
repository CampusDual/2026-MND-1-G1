package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.CreateReceptionReservationRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionAvailabilityDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionBookingCreatedDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionGroupOptionDto;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionBookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints de alta y disponibilidad de reservas para recepción.
 */
@Validated
@RestController
@RequestMapping("/api/recepcion")
public class ReceptionBookingController {

    private final ReceptionBookingService receptionBookingService;

    public ReceptionBookingController(ReceptionBookingService receptionBookingService) {
        this.receptionBookingService = receptionBookingService;
    }

    /**
     * Lista los grupos del centro autenticado para selección explícita.
     *
     * @param principal usuario autenticado
     * @return grupos configurados
     */
    @GetMapping("/grupos")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<List<ReceptionGroupOptionDto>> getGroups(Principal principal) {
        return ResponseEntity.ok(this.receptionBookingService.findGroups(principal.getName()));
    }

    /**
     * Consulta la ocupación informativa de un grupo y fecha.
     *
     * @param date fecha consultada
     * @param groupId grupo seleccionado
     * @param principal usuario autenticado
     * @return capacidad y plazas restantes por franja
     */
    @GetMapping("/disponibilidad")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<ReceptionAvailabilityDto> getAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @PositiveOrZero Long groupId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                this.receptionBookingService.findAvailability(date, groupId, principal.getName())
        );
    }

    /**
     * Crea una reserva confirmada dentro del centro autenticado.
     *
     * @param request datos permitidos de la reserva
     * @param principal usuario autenticado
     * @param uriBuilder constructor de la ruta del detalle
     * @return reserva creada con su cabecera Location
     */
    @PostMapping("/reservas")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<ReceptionBookingCreatedDto> createReservation(
            @Valid @RequestBody CreateReceptionReservationRequestDto request,
            Principal principal,
            UriComponentsBuilder uriBuilder
    ) {
        ReceptionBookingCreatedDto created = this.receptionBookingService.createReservation(
                request,
                principal.getName()
        );
        return ResponseEntity.created(
                uriBuilder.path("/api/recepcion/reservas/{id}")
                        .buildAndExpand(created.id())
                        .toUri()
        ).body(created);
    }
}
