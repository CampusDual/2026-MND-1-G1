package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.ReceptionReservationDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionReservationPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.CreateReceptionArrivalRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.RegisterReceptionDepartureRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionDepartureDataDto;
import com.campusdual.cd2026mnd1g1.api.service.DaycareOperationsService;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final DaycareOperationsService daycareOperationsService;

    public ReceptionReservationController(
            ReceptionReservationService receptionReservationService,
            DaycareOperationsService daycareOperationsService
    ) {
        this.receptionReservationService = receptionReservationService;
        this.daycareOperationsService = daycareOperationsService;
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

    /**
     * Recupera los datos mínimos para preparar una salida.
     *
     * @param id identificador de la reserva
     * @param principal usuario autenticado
     * @return datos operativos y personas autorizadas
     */
    @GetMapping("/{id}/datos-salida")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<ReceptionDepartureDataDto> getDepartureData(
            @PathVariable @Min(0) Long id,
            Principal principal
    ) {
        return ResponseEntity.ok(this.receptionReservationService.findDepartureData(id, principal.getName()));
    }

    /**
     * Registra la llegada de una reserva del día en el centro autenticado.
     *
     * @param id identificador de la reserva
     * @param request observaciones opcionales, incluidas las pertenencias que deja el tutor
     * @param principal usuario autenticado
     * @return entrada persistida
     */
    @PostMapping("/{id}/entrada")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<CheckRecordDto> registerArrival(
            @PathVariable @Min(0) Long id,
            @Valid @RequestBody CreateReceptionArrivalRequestDto request,
            Principal principal
    ) {
        CheckRecordDto record = this.daycareOperationsService.registerArrival(
                id,
                request,
                principal.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    /**
     * Registra atómicamente la salida y el destinatario de la entrega.
     *
     * @param id identificador de la reserva
     * @param request persona que recoge y declaración de pertenencias
     * @param principal usuario autenticado
     * @return salida persistida
     */
    @PostMapping("/{id}/salida")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<CheckRecordDto> registerDeparture(
            @PathVariable @Min(0) Long id,
            @Valid @RequestBody RegisterReceptionDepartureRequestDto request,
            Principal principal
    ) {
        CheckRecordDto record = this.daycareOperationsService.registerDeparture(
                id,
                request,
                principal.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }
}
