package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.TutorCreateRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorUpdateRequestDto;
import com.campusdual.cd2026mnd1g1.api.service.TutorService;
import com.campusdual.cd2026mnd1g1.model.entity.TutorEntity;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.OperationalUserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.repository.TutorRepository;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Gestiona altas, búsquedas y cambios operativos de tutores.
 */
@Service
@Transactional
public class TutorServiceImpl implements TutorService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TutorRepository tutorRepository;
    private final UserRepository userRepository;

    public TutorServiceImpl(TutorRepository tutorRepository, UserRepository userRepository) {
        this.tutorRepository = tutorRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TutorPageDto findTutors(String search, int page, int size, String username) {
        validatePagination(page, size);
        UserEntity user = findActiveUser(username);
        String normalizedSearch = search == null ? "" : search.trim();

        Page<TutorEntity> tutorPage = normalizedSearch.isEmpty()
                ? this.tutorRepository.findByCenter_Id(
                        user.getCenter().getId(),
                        PageRequest.of(page, size, Sort.by(
                                Sort.Order.asc("firstName").ignoreCase(),
                                Sort.Order.asc("lastName").ignoreCase(),
                                Sort.Order.asc("id")
                        ))
                )
                : this.tutorRepository.searchTutors(
                        user.getCenter().getId(),
                        normalizedSearch,
                        PageRequest.of(page, size, Sort.by(
                                Sort.Order.asc("firstName").ignoreCase(),
                                Sort.Order.asc("lastName").ignoreCase(),
                                Sort.Order.asc("id")
                        ))
                );

        Page<TutorEntity> centerPage = tutorPage;

        List<TutorDto> items = centerPage.getContent().stream()
                .map(this::mapToDto)
                .toList();
        return new TutorPageDto(
                items,
                centerPage.getNumber(),
                centerPage.getSize(),
                centerPage.getTotalElements(),
                centerPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TutorDto findTutor(Long id, String username) {
        validateId(id);
        UserEntity user = findActiveUser(username);
        TutorEntity tutor = this.tutorRepository.findByIdAndCenter_Id(id, user.getCenter().getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el tutor indicado"));
        return mapToDto(tutor);
    }

    @Override
    public TutorDto createTutor(TutorCreateRequestDto request, String username) {
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la petición no puede ser nulo");
        }
        UserEntity user = findActiveUser(username);
        String firstName = normalizeText(request.firstName(), "El nombre es obligatorio");
        String lastName = normalizeText(request.lastName(), "Los apellidos son obligatorios");
        String nationalId = normalizeNationalId(request.nationalId());
        String phone = normalizeText(request.phone(), "El teléfono es obligatorio");
        String email = normalizeEmail(request.email());
        Long centerId = user.getCenter().getId();

        if (this.tutorRepository.existsByCenter_IdAndNationalIdIgnoreCase(centerId, nationalId)) {
            throw new IllegalStateException("Ya existe un tutor con ese DNI en el centro");
        }
        if (!request.confirmMatches()
                && hasPotentialMatches(centerId, phone, email, null)) {
            throw new IllegalStateException(
                    "Hay otro tutor con el mismo teléfono o email. Confirma si deseas continuar."
            );
        }

        TutorEntity tutor = new TutorEntity();
        tutor.setCenter(user.getCenter());
        tutor.setFirstName(firstName);
        tutor.setLastName(lastName);
        tutor.setNationalId(nationalId);
        tutor.setPhone(phone);
        tutor.setEmail(email);
        tutor.setCreatedAt(LocalDateTime.now());
        return mapToDto(this.tutorRepository.save(tutor));
    }

    @Override
    public TutorDto updateTutor(
            Long id,
            TutorUpdateRequestDto request,
            String ifMatch,
            String username
    ) {
        validateId(id);
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la petición no puede ser nulo");
        }
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new IllegalArgumentException("La ficha debe enviarse con su versión actual");
        }
        UserEntity user = findActiveUser(username);
        TutorEntity tutor = this.tutorRepository.findByIdAndCenter_Id(id, user.getCenter().getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el tutor indicado"));

        String currentTag = tutorTag(mapToDto(tutor));
        if (!ifMatch.trim().equals(currentTag)) {
            throw new IllegalStateException(
                    "La ficha ha cambiado desde que la abriste. Recarga sus datos antes de guardar."
            );
        }

        String firstName = normalizeText(request.firstName(), "El nombre es obligatorio");
        String lastName = normalizeText(request.lastName(), "Los apellidos son obligatorios");
        String nationalId = normalizeNationalId(request.nationalId());
        String phone = normalizeText(request.phone(), "El teléfono es obligatorio");
        String email = normalizeEmail(request.email());

        Optional<TutorEntity> dniTutor = this.tutorRepository
                .findByCenter_IdAndNationalIdIgnoreCase(user.getCenter().getId(), nationalId);
        if (dniTutor.isPresent() && !dniTutor.get().getId().equals(id)) {
            throw new IllegalStateException("Ya existe otro tutor con ese DNI en el centro");
        }
        if (tutor.getFirstName().equals(firstName)
                && tutor.getLastName().equals(lastName)
                && tutor.getNationalId().equals(nationalId)
                && tutor.getPhone().equals(phone)
                && tutor.getEmail().equals(email)) {
            return mapToDto(tutor);
        }
        if (!request.confirmMatches()
                && hasPotentialMatches(user.getCenter().getId(), phone, email, id)) {
            throw new IllegalStateException(
                    "Hay otro tutor con el mismo teléfono o email. Confirma si deseas continuar."
            );
        }

        tutor.setFirstName(firstName);
        tutor.setLastName(lastName);
        tutor.setNationalId(nationalId);
        tutor.setPhone(phone);
        tutor.setEmail(email);
        return mapToDto(this.tutorRepository.save(tutor));
    }

    public static String tutorTag(TutorDto tutor) {
        StringBuilder value = new StringBuilder();
        appendTagValue(value, String.valueOf(tutor.id()));
        appendTagValue(value, tutor.firstName());
        appendTagValue(value, tutor.lastName());
        appendTagValue(value, tutor.nationalId());
        appendTagValue(value, tutor.phone());
        appendTagValue(value, tutor.email());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8));
            return "\"" + HexFormat.of().formatHex(digest) + "\"";
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo verificar la versión de la ficha", exception);
        }
    }

    private static void appendTagValue(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value);
    }

    private boolean hasPotentialMatches(
            Long centerId,
            String phone,
            String email,
            Long excludedTutorId
    ) {
        return this.tutorRepository.findPotentialMatches(centerId, phone, email).stream()
                .filter(tutor -> excludedTutorId == null || !tutor.getId().equals(excludedTutorId))
                .findAny()
                .isPresent();
    }

    private TutorDto mapToDto(TutorEntity tutor) {
        return new TutorDto(
                tutor.getId(),
                tutor.getFirstName(),
                tutor.getLastName(),
                tutor.getNationalId(),
                tutor.getPhone(),
                tutor.getEmail()
        );
    }

    private UserEntity findActiveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new OperationalUserNotFoundException("No se pudo identificar al usuario autenticado");
        }
        UserEntity user = this.userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new OperationalUserNotFoundException(
                        "El usuario autenticado no tiene un centro asociado"
                ));
        if (!Boolean.TRUE.equals(user.getActive()) || user.getCenter() == null) {
            throw new OperationalUserNotFoundException("El usuario operativo está desactivado o sin centro");
        }
        return user;
    }

    private void validateId(Long id) {
        if (id == null || id < 0) {
            throw new IllegalArgumentException("El identificador del tutor no puede ser negativo");
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("La página debe ser cero o mayor");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y " + MAX_PAGE_SIZE);
        }
    }

    private String normalizeText(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeNationalId(String value) {
        return normalizeText(value, "El DNI es obligatorio").toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        return normalizeText(value, "El email es obligatorio").toLowerCase(Locale.ROOT);
    }
}
