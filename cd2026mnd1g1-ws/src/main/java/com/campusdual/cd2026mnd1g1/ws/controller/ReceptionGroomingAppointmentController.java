package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestOriginDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestStatusDto;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionGroomingAppointmentService;
import jakarta.validation.constraints.Max;
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
 * Endpoints de consulta de citas de peluquería para recepción.
 */
@Validated
@RestController
@RequestMapping("/api/recepcion/citas-peluqueria")
public class ReceptionGroomingAppointmentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReceptionGroomingAppointmentService appointmentService;

    public ReceptionGroomingAppointmentController(ReceptionGroomingAppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Lista citas del centro del usuario autenticado, aplicando los filtros indicados.
     *
     * @param origin filtro opcional por origen
     * @param status filtro opcional por estado
     * @param from inicio inclusivo del rango de fechas
     * @param to fin inclusivo del rango de fechas
     * @param page índice de página empezando en cero
     * @param size número de resultados por página
     * @param principal usuario autenticado
     * @return página de citas
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<GroomingAppointmentPageDto> getAppointments(
            @RequestParam(required = false) GroomingRequestOriginDto origin,
            @RequestParam(required = false) GroomingRequestStatusDto status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            Principal principal
    ) {
        return ResponseEntity.ok(this.appointmentService.findAppointments(
                origin,
                status,
                from,
                to,
                page,
                size,
                principal.getName()
        ));
    }

    /**
     * Consulta una cita solo cuando pertenece al ámbito del centro autenticado.
     *
     * @param id identificador de cita
     * @param principal usuario autenticado
     * @return detalle de cita
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<GroomingAppointmentDetailDto> getAppointment(
            @PathVariable @Min(0) Long id,
            Principal principal
    ) {
        return ResponseEntity.ok(this.appointmentService.findAppointment(id, principal.getName()));
    }
}
