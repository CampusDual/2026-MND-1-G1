package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.TutorCreateRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorUpdateRequestDto;
import com.campusdual.cd2026mnd1g1.api.service.TutorService;
import com.campusdual.cd2026mnd1g1.model.service.TutorServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;

/**
 * Endpoints de gestión de tutores para recepción.
 */
@Validated
@RestController
@RequestMapping("/api/recepcion/tutores")
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    /**
     * Lista tutores del centro autorizado.
     *
     * @param search texto opcional de búsqueda
     * @param page página solicitada
     * @param size tamaño de página
     * @param principal usuario autenticado
     * @return página de tutores
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<TutorPageDto> getTutors(
            @RequestParam(required = false) @Size(max = 254) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            Principal principal
    ) {
        return ResponseEntity.ok(
                this.tutorService.findTutors(search, page, size, principal.getName())
        );
    }

    /**
     * Recupera una ficha de tutor.
     *
     * @param id identificador del tutor
     * @param principal usuario autenticado
     * @return ficha y etiqueta de versión
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<TutorDto> getTutor(
            @PathVariable @Min(0) Long id,
            Principal principal
    ) {
            TutorDto tutor = this.tutorService.findTutor(id, principal.getName());
            HttpHeaders headers = new HttpHeaders();
            headers.setETag(TutorServiceImpl.tutorTag(tutor));
        return ResponseEntity.ok().headers(headers).body(tutor);
    }

    /**
     * Registra un tutor en el centro autorizado.
     *
     * @param request datos del tutor
     * @param principal usuario autenticado
     * @param uriBuilder constructor de URI
     * @return tutor creado
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<TutorDto> createTutor(
            @Valid @RequestBody TutorCreateRequestDto request,
            Principal principal,
            UriComponentsBuilder uriBuilder
    ) {
        TutorDto created = this.tutorService.createTutor(request, principal.getName());
        return ResponseEntity.created(
                uriBuilder.path("/api/recepcion/tutores/{id}")
                        .buildAndExpand(created.id())
                        .toUri()
        ).body(created);
    }

    /**
     * Actualiza los datos operativos permitidos de un tutor.
     *
     * @param id identificador del tutor
     * @param ifMatch etiqueta de versión recibida al consultar la ficha
     * @param request datos modificados
     * @param principal usuario autenticado
     * @return tutor actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<TutorDto> updateTutor(
            @PathVariable @Min(0) Long id,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody TutorUpdateRequestDto request,
            Principal principal
    ) {
        TutorDto updated = this.tutorService.updateTutor(
                id,
                request,
                ifMatch,
                principal.getName()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setETag(TutorServiceImpl.tutorTag(updated));
        return ResponseEntity.ok().headers(headers).body(updated);
    }
}
