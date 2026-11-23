package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReservationDayDto;
import com.campusdual.cd2026mnd1g1.api.service.DaycareOperationsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/reservas")
public class DaycareOperationsController {

    private final DaycareOperationsService daycareOperationsService;

    public DaycareOperationsController(DaycareOperationsService daycareOperationsService) {
        this.daycareOperationsService = daycareOperationsService;
    }

    /**
     * Recupera las reservas del día del centro del usuario autenticado.
     *
     * @param date fecha opcional, por defecto el día actual
     * @param groupId filtro opcional por grupo
     * @param principal usuario autenticado
     * @return reservas con ficha de perro y ocupación
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<List<ReservationDayDto>> getDailyReservations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @Positive Long groupId,
            Principal principal
    ) {
        String username = principal == null ? null : principal.getName();
        return ResponseEntity.ok(this.daycareOperationsService.findReservations(date, groupId, username));
    }

    /**
     * Registra la entrada o salida de una reserva.
     *
     * @param id identificador de la reserva
     * @param request datos del registro
     * @param principal usuario autenticado
     * @return registro creado y estado actualizado de la reserva
     */
    @PostMapping("/{id}/check-registros")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<CheckRecordDto> createCheckRecord(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CheckRecordRequestDto request,
            Principal principal
    ) {
        String username = principal == null ? null : principal.getName();
        CheckRecordDto record = this.daycareOperationsService.createCheckRecord(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    /**
     * Registra una actividad diaria asociada a una reserva.
     *
     * @param id identificador de la reserva
     * @param request datos de la actividad
     * @param principal usuario autenticado
     * @return registro diario creado
     */
    @PostMapping("/{id}/registros-diarios")
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<DailyRecordDto> createDailyRecord(
            @PathVariable @Positive Long id,
            @Valid @RequestBody DailyRecordRequestDto request,
            Principal principal
    ) {
        String username = principal == null ? null : principal.getName();
        DailyRecordDto record = this.daycareOperationsService.createDailyRecord(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    /**
     * Registra una incidencia asociada a una reserva.
     *
     * @param id identificador de la reserva
     * @param request datos de la incidencia
     * @param principal usuario autenticado
     * @return incidencia creada
     */
    @PostMapping("/{id}/incidencias")
    @PreAuthorize("hasRole('CUIDADOR')")
    public ResponseEntity<IncidentDto> createIncident(
            @PathVariable @Positive Long id,
            @Valid @RequestBody IncidentRequestDto request,
            Principal principal
    ) {
        String username = principal == null ? null : principal.getName();
        IncidentDto incident = this.daycareOperationsService.createIncident(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(incident);
    }
}
