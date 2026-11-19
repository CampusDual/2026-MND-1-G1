package com.campusdual.cd2026mnd1g1.ws.controller;

import com.campusdual.cd2026mnd1g1.api.dto.TutorDogDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogCreateRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogCreateResultDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogEditResultDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogUpdateRequestDto;
import com.campusdual.cd2026mnd1g1.api.service.TutorPortalService;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.api.dto.TutorAuthorizedPersonDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.security.Principal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consultas privadas del tutor autenticado.
 */
@Validated
@RestController
@RequestMapping("/api/tutor/mis-perros")
public class TutorPortalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TutorPortalController.class);
    private static final Map<String, MediaType> IMAGE_TYPES = Map.of(
            ".jpg", MediaType.IMAGE_JPEG,
            ".jpeg", MediaType.IMAGE_JPEG,
            ".png", MediaType.IMAGE_PNG,
            ".gif", MediaType.IMAGE_GIF,
            ".webp", MediaType.parseMediaType("image/webp")
    );

    private final TutorPortalService tutorPortalService;
    private final String photoDirectory;
    private final TutorDogPhotoStorage photoStorage;

    public TutorPortalController(
            TutorPortalService tutorPortalService,
            @Value("${app.dogs.photos.directory:}") String photoDirectory,
            TutorDogPhotoStorage photoStorage
    ) {
        this.tutorPortalService = tutorPortalService;
        this.photoDirectory = photoDirectory;
        this.photoStorage = photoStorage;
    }

    /**
     * Lista únicamente los perros de la ficha vinculada a la cuenta autenticada.
     *
     * @param page índice de página
     * @param size número de perros por página
     * @param principal identidad autenticada
     * @return página de perros del tutor
     */
    @GetMapping
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<TutorDogPageDto> getMyDogs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            Principal principal
    ) {
        return ResponseEntity.ok(this.tutorPortalService.findMyDogs(principal.getName(), page, size));
    }

    /**
     * Consulta las autorizaciones generales del tutor autenticado.
     *
     * @param principal identidad autenticada
     * @return nombres y teléfonos de las personas autorizadas
     */
    @GetMapping("/personas-autorizadas")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<List<TutorAuthorizedPersonDto>> getMyAuthorizedPersons(Principal principal) {
        return ResponseEntity.ok(this.tutorPortalService.findMyAuthorizedPersons(principal.getName()));
    }

    /**
     * Recupera una ficha solo si el perro pertenece al tutor autenticado.
     *
     * @param id identificador del perro
     * @param principal identidad autenticada
     * @return campos compartidos de la ficha
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<TutorDogDetailDto> getMyDog(
            @PathVariable @Min(0) Long id,
            Principal principal
    ) {
        TutorDogEditResultDto result = this.tutorPortalService.findMyDogVersioned(id, principal.getName());
        return ResponseEntity.ok().eTag(result.etag()).body(result.dog());
    }

    /**
     * Actualiza los campos permitidos de una ficha propia y su foto opcional.
     *
     * @param id identificador del perro
     * @param request datos permitidos
     * @param photo foto opcional de reemplazo
     * @param ifMatch versión leída por el cliente
     * @param principal identidad autenticada
     * @return ficha actualizada y nueva etiqueta de versión
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<TutorDogDetailDto> updateMyDog(
            @PathVariable @Min(0) Long id,
            @Valid @RequestPart("request") TutorDogUpdateRequestDto request,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            Principal principal
    ) {
        if (request.removePhoto() && photo != null && !photo.isEmpty()) {
            throw new IllegalArgumentException("No se puede reemplazar y retirar la foto en la misma operación");
        }
        String replacementPhotoReference = this.photoStorage.store(photo);
        TutorDogEditResultDto result;
        try {
            result = this.tutorPortalService.updateMyDog(
                    id,
                    request,
                    principal.getName(),
                    ifMatch,
                    replacementPhotoReference
            );
        } catch (RuntimeException exception) {
            cleanupPhoto(replacementPhotoReference, exception);
            throw exception;
        }
        if (result.photoChanged()) {
            cleanupOldPhoto(result.previousPhotoReference());
        }
        return ResponseEntity.ok().eTag(result.etag()).body(result.dog());
    }

    /**
     * Crea un perro y sus datos asociados para el tutor autenticado.
     *
     * @param request datos permitidos del perro
     * @param photo foto opcional
     * @param idempotencyKey clave estable para reintentos
     * @param principal identidad autenticada
     * @param uriBuilder constructor de la ruta de detalle
     * @return detalle del perro creado o resultado previo
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<TutorDogDetailDto> createMyDog(
            @Valid @RequestPart("request") TutorDogCreateRequestDto request,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Principal principal,
            UriComponentsBuilder uriBuilder
    ) {
        String normalizedKey;
        try {
            normalizedKey = UUID.fromString(idempotencyKey).toString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("La clave de envío debe ser un UUID válido", exception);
        }

        byte[] imageBytes = readPhotoBytes(photo);
        String payloadHash = createPayloadHash(request, imageBytes);
        String photoReference = this.photoStorage.store(photo);
        TutorDogCreateResultDto result;
        try {
            result = this.tutorPortalService.createMyDog(
                    request,
                    principal.getName(),
                    normalizedKey,
                    payloadHash,
                    photoReference
            );
        } catch (RuntimeException exception) {
            cleanupPhoto(photoReference, exception);
            throw exception;
        }
        if (result.replayed()) {
            cleanupPhoto(photoReference, null);
            return ResponseEntity.ok()
                    .header("Idempotency-Replayed", "true")
                    .body(result.dog());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(uriBuilder.path("/api/tutor/mis-perros/{id}")
                        .buildAndExpand(result.dog().id())
                        .toUri())
                .body(result.dog());
    }

    /**
     * Sirve una foto local solo tras comprobar que el perro pertenece al tutor.
     *
     * @param id identificador del perro
     * @param principal identidad autenticada
     * @return contenido de imagen del directorio privado configurado
     */
    @GetMapping("/{id}/foto")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<Resource> getMyDogPhoto(@PathVariable @Min(0) Long id, Principal principal) {
        String photoReference = this.tutorPortalService.findMyDogPhotoReference(id, principal.getName());
        Path photo = resolvePhoto(photoReference);
        MediaType mediaType = resolveMediaType(photo);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"dog-photo\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(photo));
    }

    private Path resolvePhoto(String photoReference) {
        if (this.photoDirectory == null || this.photoDirectory.isBlank()
                || photoReference == null
                || !photoReference.matches("(?i)[a-z0-9][a-z0-9._-]{0,127}\\.(jpg|jpeg|png|gif|webp)")) {
            throw new ResourceNotFoundException("No hay una foto disponible para este perro");
        }
        try {
            Path root = Path.of(this.photoDirectory).toRealPath();
            Path candidate = root.resolve(photoReference).normalize();
            if (!candidate.startsWith(root)) {
                throw new ResourceNotFoundException("No hay una foto disponible para este perro");
            }
            Path realPhoto = candidate.toRealPath();
            if (!realPhoto.startsWith(root) || !Files.isRegularFile(realPhoto)) {
                throw new ResourceNotFoundException("No hay una foto disponible para este perro");
            }
            return realPhoto;
        } catch (IOException | SecurityException exception) {
            throw new ResourceNotFoundException("No hay una foto disponible para este perro");
        }
    }

    private MediaType resolveMediaType(Path photo) {
        String fileName = photo.getFileName().toString().toLowerCase(Locale.ROOT);
        int extensionStart = fileName.lastIndexOf('.');
        MediaType mediaType = extensionStart < 0 ? null : IMAGE_TYPES.get(fileName.substring(extensionStart));
        if (mediaType == null) {
            throw new ResourceNotFoundException("No hay una foto disponible para este perro");
        }
        return mediaType;
    }

    private byte[] readPhotoBytes(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return new byte[0];
        }
        if (photo.getSize() > 5L * 1024L * 1024L) {
            throw new IllegalArgumentException("La foto no puede superar 5 MB");
        }
        try {
            return photo.getBytes();
        } catch (IOException exception) {
            throw new TutorDogPhotoStorageException("No se pudo leer la foto enviada", exception);
        }
    }

    private String createPayloadHash(TutorDogCreateRequestDto request, byte[] imageBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateDigest(digest, request.name());
            updateDigest(digest, request.breed());
            updateDigest(digest, request.size());
            updateDigest(digest, request.birthDate() == null ? null : request.birthDate().toString());
            updateDigest(digest, request.allergies() == null ? -1 : request.allergies().size());
            if (request.allergies() != null) {
                request.allergies().forEach(item ->
                        updateDigest(digest, item == null ? null : item.description()));
            }

            updateDigest(digest, request.medications() == null ? -1 : request.medications().size());
            if (request.medications() != null) {
                request.medications().forEach(item -> {
                    updateDigest(digest, item == null ? null : item.medication());
                    updateDigest(digest, item == null ? null : item.dosage());
                    updateDigest(digest, item == null ? null : item.instructions());
                });
            }
            updateDigest(digest, request.authorizedPersons() == null ? -1 : request.authorizedPersons().size());
            if (request.authorizedPersons() != null) {
                request.authorizedPersons().forEach(item -> {
                    updateDigest(digest, item == null ? null : item.name());
                    updateDigest(digest, item == null ? null : item.phone());
                });
            }
            digest.update(intBytes(imageBytes.length));
            digest.update(imageBytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo preparar la clave segura de envío", exception);
        }
    }

    private void updateDigest(MessageDigest digest, String value) {
        if (value == null) {
            digest.update(intBytes(-1));
            return;
        }
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        digest.update(intBytes(bytes.length));
        digest.update(bytes);
    }

    private void updateDigest(MessageDigest digest, int value) {
        digest.update(intBytes(value));
    }

    private byte[] intBytes(int value) {
        return new byte[] {
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    private void cleanupPhoto(String photoReference, RuntimeException originalException) {
        try {
            this.photoStorage.delete(photoReference);
        } catch (RuntimeException cleanupException) {
            if (originalException != null) {
                originalException.addSuppressed(cleanupException);
                return;
            }
            throw cleanupException;
        }
    }

    private void cleanupOldPhoto(String photoReference) {
        try {
            this.photoStorage.delete(photoReference);
        } catch (RuntimeException exception) {
            LOGGER.error("La foto anterior del perro no se pudo limpiar; la actualización ya fue guardada", exception);
        }
    }
}
