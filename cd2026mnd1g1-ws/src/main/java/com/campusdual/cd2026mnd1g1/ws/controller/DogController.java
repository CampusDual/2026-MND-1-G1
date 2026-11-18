package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.DogCreateRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.DogDto;
import com.campusdual.cd2026mnd1g1.api.dto.DogPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.DogUpdateRequestDto;
import com.campusdual.cd2026mnd1g1.api.service.DogService;
import com.campusdual.cd2026mnd1g1.model.service.DogServiceImpl;
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
 * Endpoints de gestión interna de perros para recepción.
 */
@Validated
@RestController
@RequestMapping("/api/recepcion/perros")
public class DogController {

    private final DogService dogService;

    public DogController(DogService dogService) {
        this.dogService = dogService;
    }

    /**
     * Lista o busca perros del centro autorizado.
     *
     * @param search texto opcional de búsqueda
     * @param page índice de página
     * @param size número de resultados
     * @param principal usuario autenticado
     * @return resultados paginados
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<DogPageDto> getDogs(
            @RequestParam(required = false) @Size(max = 254) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            Principal principal
    ) {
        return ResponseEntity.ok(this.dogService.findDogs(search, page, size, principal.getName()));
    }

    /**
     * Recupera una ficha y su tutor en el centro autorizado.
     *
     * @param id identificador del perro
     * @param principal usuario autenticado
     * @return ficha con su versión
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<DogDto> getDog(@PathVariable @Min(0) Long id, Principal principal) {
        DogDto dog = this.dogService.findDog(id, principal.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setETag(DogServiceImpl.dogTag(dog));
        return ResponseEntity.ok().headers(headers).body(dog);
    }

    /**
     * Registra un perro asociado a un tutor del centro autorizado.
     *
     * @param request datos del perro
     * @param principal usuario autenticado
     * @param uriBuilder constructor de URI
     * @return ficha creada
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<DogDto> createDog(
            @Valid @RequestBody DogCreateRequestDto request,
            Principal principal,
            UriComponentsBuilder uriBuilder
    ) {
        DogDto created = this.dogService.createDog(request, principal.getName());
        return ResponseEntity.created(
                uriBuilder.path("/api/recepcion/perros/{id}")
                        .buildAndExpand(created.id())
                        .toUri()
        ).body(created);
    }

    /**
     * Actualiza solo los datos básicos permitidos de un perro.
     *
     * @param id identificador del perro
     * @param ifMatch versión leída antes de editar
     * @param request datos editables
     * @param principal usuario autenticado
     * @return ficha actualizada y su nueva versión
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'GERENTE')")
    public ResponseEntity<DogDto> updateDog(
            @PathVariable @Min(0) Long id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody DogUpdateRequestDto request,
            Principal principal
    ) {
        DogDto updated = this.dogService.updateDog(id, request, ifMatch, principal.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setETag(DogServiceImpl.dogTag(updated));
        return ResponseEntity.ok().headers(headers).body(updated);
    }
}
