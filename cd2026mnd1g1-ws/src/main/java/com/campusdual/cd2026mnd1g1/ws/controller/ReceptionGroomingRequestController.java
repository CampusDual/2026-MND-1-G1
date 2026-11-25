package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestOriginDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestStatusDto;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionGroomingRequestService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;

/**
 * Endpoints de consulta de solicitudes de peluquería para recepción.
 */
@Validated
@RestController
@RequestMapping("/api/recepcion/solicitudes-peluqueria")
public class ReceptionGroomingRequestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReceptionGroomingRequestService receptionGroomingRequestService;

    public ReceptionGroomingRequestController(
            ReceptionGroomingRequestService receptionGroomingRequestService
    ) {
        this.receptionGroomingRequestService = receptionGroomingRequestService;
    }

    /**
     * Lista solicitudes dentro del ámbito del centro del usuario autenticado.
     *
     * @param origin filtro opcional por origen
     * @param status filtro opcional por estado
     * @param fromDate inicio opcional del rango de fechas
     * @param toDate fin opcional del rango de fechas
     * @param page índice de página empezando en cero
     * @param size número de resultados por página
     * @param principal usuario autenticado
     * @return página de solicitudes
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<GroomingRequestPageDto> getRequests(
            @RequestParam(required = false) GroomingRequestOriginDto origin,
            @RequestParam(required = false) GroomingRequestStatusDto status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            Principal principal
    ) {
        return ResponseEntity.ok(this.receptionGroomingRequestService.findRequests(
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
     * Consulta una solicitud solo cuando sus servicios pertenecen al centro autenticado.
     *
     * @param id identificador de solicitud
     * @param principal usuario autenticado
     * @return detalle de la solicitud
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<GroomingRequestDetailDto> getRequest(
            @PathVariable @Min(0) Long id,
            Principal principal
    ) {
        return ResponseEntity.ok(this.receptionGroomingRequestService.findRequest(id, principal.getName()));
    }

    /**
     * Confirma una solicitud pendiente del centro autenticado.
     *
     * @param id identificador de solicitud
     * @param principal usuario autenticado
     * @return solicitud con el estado actualizado
     */
    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<GroomingRequestDetailDto> confirmRequest(
            @PathVariable @Min(0) Long id,
            Principal principal
    ) {
        return ResponseEntity.ok(this.receptionGroomingRequestService.confirmRequest(id, principal.getName()));
    }

    /**
     * Rechaza una solicitud pendiente del centro autenticado.
     *
     * @param id identificador de solicitud
     * @param principal usuario autenticado
     * @return solicitud con el estado actualizado
     */
    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<GroomingRequestDetailDto> rejectRequest(
            @PathVariable @Min(0) Long id,
            Principal principal
    ) {
        return ResponseEntity.ok(this.receptionGroomingRequestService.rejectRequest(id, principal.getName()));
    }
}
