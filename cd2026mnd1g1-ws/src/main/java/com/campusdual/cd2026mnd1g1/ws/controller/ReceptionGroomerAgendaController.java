package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.GroomerAgendaDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomerOptionDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentDetailDto;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionGroomingAppointmentService;
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
import java.util.List;

/**
 * Endpoints de consulta de agenda de peluqueros para recepción.
 */
@Validated
@RestController
@RequestMapping("/api/recepcion/peluqueros")
public class ReceptionGroomerAgendaController {

    private final ReceptionGroomingAppointmentService appointmentService;

    public ReceptionGroomerAgendaController(ReceptionGroomingAppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Lista peluqueros activos del centro de la recepcionista autenticada.
     *
     * @param principal usuario autenticado
     * @return peluqueros disponibles
     */
    @GetMapping
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<List<GroomerOptionDto>> getGroomers(Principal principal) {
        return ResponseEntity.ok(this.appointmentService.findGroomers(principal.getName()));
    }

    /**
     * Consulta las citas asignadas a un peluquero para un día concreto.
     *
     * @param groomerId identificador del peluquero
     * @param date día consultado
     * @param principal usuario autenticado
     * @return agenda de solo lectura
     */
    @GetMapping("/{groomerId}/agenda")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<GroomerAgendaDto> getAgenda(
            @PathVariable @Min(0) Long groomerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Principal principal
    ) {
        return ResponseEntity.ok(this.appointmentService.findGroomerAgenda(
                groomerId,
                date,
                principal.getName()
        ));
    }

    /**
     * Consulta una cita únicamente si está asignada al peluquero indicado.
     *
     * @param groomerId identificador del peluquero
     * @param appointmentId identificador de cita
     * @param principal usuario autenticado
     * @return detalle de cita
     */
    @GetMapping("/{groomerId}/agenda/{appointmentId}")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<GroomingAppointmentDetailDto> getAgendaAppointment(
            @PathVariable @Min(0) Long groomerId,
            @PathVariable @Min(0) Long appointmentId,
            Principal principal
    ) {
        return ResponseEntity.ok(this.appointmentService.findGroomerAgendaAppointment(
                groomerId,
                appointmentId,
                principal.getName()
        ));
    }
}
