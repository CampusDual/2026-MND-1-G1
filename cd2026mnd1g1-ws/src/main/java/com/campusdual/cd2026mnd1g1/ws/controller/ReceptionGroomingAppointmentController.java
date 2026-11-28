package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.CreateGroomingAppointmentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.AssignGroomingAppointmentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingDogOptionPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingServiceOptionDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestOriginDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentStatusDto;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionGroomingAppointmentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

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
     * Lista perros de la guardería interna disponibles para el formulario.
     *
     * @param search búsqueda opcional por nombre
     * @param page índice de página
     * @param size número de resultados
     * @param principal usuario autenticado
     * @return página de perros del centro
     */
    @GetMapping("/perros")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<GroomingDogOptionPageDto> getInternalDogs(
            @RequestParam(required = false) @Size(max = 120) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            Principal principal
    ) {
        return ResponseEntity.ok(this.appointmentService.findInternalDogs(
                search,
                page,
                size,
                principal.getName()
        ));
    }

    /**
     * Lista los servicios activos que ofrece el centro autenticado.
     *
     * @param principal usuario autenticado
     * @return servicios disponibles
     */
    @GetMapping("/servicios")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<List<GroomingServiceOptionDto>> getAvailableServices(Principal principal) {
        return ResponseEntity.ok(this.appointmentService.findAvailableServices(principal.getName()));
    }

    /**
     * Crea una cita interna sin asignar peluquero ni lugar.
     *
     * @param request datos permitidos de la cita
     * @param principal usuario autenticado
     * @param uriBuilder constructor de URI
     * @return detalle persistido de la cita
     */
    @PostMapping
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<GroomingAppointmentDetailDto> createAppointment(
            @Valid @RequestBody CreateGroomingAppointmentRequestDto request,
            Principal principal,
            UriComponentsBuilder uriBuilder
    ) {
        GroomingAppointmentDetailDto created = this.appointmentService.createAppointment(
                request,
                principal.getName()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriBuilder.path("/api/recepcion/citas-peluqueria/{id}")
                .buildAndExpand(created.id())
                .toUri());
        return ResponseEntity.created(headers.getLocation()).headers(headers).body(created);
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
            @RequestParam(required = false) GroomingAppointmentStatusDto status,
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

    /**
     * Asigna peluquero y lugar a una cita pendiente de asignación.
     *
     * @param id identificador de cita
     * @param request campos permitidos de asignación
     * @param principal usuario autenticado
     * @return cita asignada y persistida
     */
    @PostMapping("/{id}/asignacion")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<GroomingAppointmentDetailDto> assignAppointment(
            @PathVariable @Min(0) Long id,
            @Valid @RequestBody AssignGroomingAppointmentRequestDto request,
            Principal principal
    ) {
        return ResponseEntity.ok(this.appointmentService.assignAppointment(
                id,
                request,
                principal.getName()
        ));
    }
}
