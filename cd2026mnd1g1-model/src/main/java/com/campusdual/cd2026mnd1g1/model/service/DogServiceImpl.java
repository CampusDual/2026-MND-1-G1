package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.DogCreateRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.AuthorizedPersonDto;
import com.campusdual.cd2026mnd1g1.api.dto.DogDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.DogDto;
import com.campusdual.cd2026mnd1g1.api.dto.DogPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.DogUpdateRequestDto;
import com.campusdual.cd2026mnd1g1.api.service.DogService;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DogSize;
import com.campusdual.cd2026mnd1g1.model.repository.AuthorizedPersonRepository;
import com.campusdual.cd2026mnd1g1.model.entity.TutorEntity;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.OperationalUserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.repository.DogRepository;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Gestiona las fichas básicas de perros dentro del centro autorizado.
 */
@Service
@Transactional
public class DogServiceImpl implements DogService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DUPLICATE_MESSAGE =
            "Ya existe un perro con el mismo nombre para ese tutor. Confirma si quieres continuar.";

    private final DogRepository dogRepository;
    private final AuthorizedPersonRepository authorizedPersonRepository;
    private final TutorRepository tutorRepository;
    private final UserRepository userRepository;
    private final Clock receptionClock;

    public DogServiceImpl(
            DogRepository dogRepository,
            AuthorizedPersonRepository authorizedPersonRepository,
            TutorRepository tutorRepository,
            UserRepository userRepository,
            Clock receptionClock
    ) {
        this.dogRepository = dogRepository;
        this.authorizedPersonRepository = authorizedPersonRepository;
        this.tutorRepository = tutorRepository;
        this.userRepository = userRepository;
        this.receptionClock = receptionClock;
    }

    @Override
    @Transactional(readOnly = true)
    public DogPageDto findDogs(String search, int page, int size, String username) {
        return findDogs(search, page, size, username, false);
    }

    @Override
    @Transactional(readOnly = true)
    public DogPageDto findReservationCandidates(String search, int page, int size, String username) {
        return findDogs(search, page, size, username, true);
    }

    private DogPageDto findDogs(
            String search,
            int page,
            int size,
            String username,
            boolean reservationCandidatesOnly
    ) {
        validatePagination(page, size);
        UserEntity user = findActiveUser(username);
        String normalizedSearch = search == null ? "" : search.trim();
        Sort sort = Sort.by(
                Sort.Order.asc("name").ignoreCase(),
                Sort.Order.asc("id")
        );
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<DogEntity> dogPage;
        if (reservationCandidatesOnly) {
            dogPage = normalizedSearch.isEmpty()
                    ? this.dogRepository.findReservationCandidates(user.getCenter().getId(), pageRequest)
                    : this.dogRepository.searchReservationCandidates(
                            user.getCenter().getId(),
                            normalizedSearch,
                            pageRequest
                    );
        } else {
            dogPage = normalizedSearch.isEmpty()
                    ? this.dogRepository.findAllByCenter(user.getCenter().getId(), pageRequest)
                    : this.dogRepository.searchDogs(
                            user.getCenter().getId(),
                            normalizedSearch,
                            pageRequest
                    );
        }

        List<DogDto> items = dogPage.getContent().stream().map(this::mapToDto).toList();
        return new DogPageDto(
                items,
                dogPage.getNumber(),
                dogPage.getSize(),
                dogPage.getTotalElements(),
                dogPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DogDetailDto findDog(Long id, String username) {
        validateId(id);
        UserEntity user = findActiveUser(username);
        DogEntity dog = this.dogRepository.findByIdAndCenter_Id(id, user.getCenter().getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el perro indicado"));
        validateTutorCenter(dog);
        List<AuthorizedPersonDto> authorizedPersons = dog.getTutor() == null
                ? List.of()
                : this.authorizedPersonRepository
                        .findByTutor_IdOrderByNameAscIdAsc(dog.getTutor().getId())
                        .stream()
                        .map(person -> new AuthorizedPersonDto(person.getId(), person.getName()))
                        .toList();
        return new DogDetailDto(mapToDto(dog), authorizedPersons);
    }

    @Override
    public DogDto createDog(DogCreateRequestDto request, String username) {
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la petición no puede ser nulo");
        }
        UserEntity user = findActiveUser(username);
        Long centerId = user.getCenter().getId();
        TutorEntity tutor = findTutor(request.tutorId(), centerId);
        String name = normalizeRequiredText(request.name(), "El nombre del perro es obligatorio");
        String breed = normalizeOptionalText(request.breed());
        DogSize size = parseSize(request.size());
        validateBirthDate(request.birthDate());
        validateBreedLength(breed);

        if (!request.confirmMatches() && hasPotentialDuplicates(centerId, tutor.getId(), name, null)) {
            throw new IllegalStateException(DUPLICATE_MESSAGE);
        }

        DogEntity dog = new DogEntity();
        dog.setCenter(user.getCenter());
        dog.setTutor(tutor);
        dog.setName(name);
        dog.setBreed(breed);
        dog.setSize(size);
        dog.setBirthDate(request.birthDate());
        dog.setActive(true);
        dog.setInformationDeclaredByTutor(false);
        dog.setCreatedAt(LocalDateTime.now(this.receptionClock));
        return mapToDto(this.dogRepository.save(dog));
    }

    @Override
    public DogDto updateDog(Long id, DogUpdateRequestDto request, String ifMatch, String username) {
        validateId(id);
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la petición no puede ser nulo");
        }
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new IllegalArgumentException("La ficha debe enviarse con su versión actual");
        }
        UserEntity user = findActiveUser(username);
        Long centerId = user.getCenter().getId();
        DogEntity dog = this.dogRepository.findByIdAndCenter_Id(id, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el perro indicado"));
        validateTutorCenter(dog);
        if (!ifMatch.trim().equals(dogTag(mapToDto(dog)))) {
            throw new IllegalStateException(
                    "La ficha del perro ha cambiado desde que la abriste. Recarga sus datos antes de guardar."
            );
        }

        String name = normalizeRequiredText(request.getName(), "El nombre del perro es obligatorio");
        String breed = updatedBreed(request, dog);
        DogSize size = parseSize(request.getSize());
        LocalDate birthDate = updatedBirthDate(request, dog);
        validateBirthDate(birthDate);
        validateBreedLength(breed);

        if (dog.getName().equals(name)
                && Objects.equals(dog.getBreed(), breed)
                && dog.getSize() == size
                && Objects.equals(dog.getBirthDate(), birthDate)) {
            return mapToDto(dog);
        }
        Long tutorId = dog.getTutor() == null ? null : dog.getTutor().getId();
        if (tutorId != null
                && !request.isConfirmMatches()
                && hasPotentialDuplicates(centerId, tutorId, name, id)) {
            throw new IllegalStateException(DUPLICATE_MESSAGE);
        }

        dog.setName(name);
        dog.setBreed(breed);
        dog.setSize(size);
        dog.setBirthDate(birthDate);
        return mapToDto(this.dogRepository.save(dog));
    }

    public static String dogTag(DogDto dog) {
        StringBuilder value = new StringBuilder();
        appendTagValue(value, String.valueOf(dog.id()));
        appendTagValue(value, dog.name());
        appendTagValue(value, String.valueOf(dog.breed()));
        appendTagValue(value, dog.size());
        appendTagValue(value, String.valueOf(dog.birthDate()));
        appendTagValue(value, String.valueOf(dog.tutorId()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8));
            return "\"" + HexFormat.of().formatHex(digest) + "\"";
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo verificar la versión de la ficha del perro", exception);
        }
    }

    private static void appendTagValue(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value);
    }

    private boolean hasPotentialDuplicates(
            Long centerId,
            Long tutorId,
            String name,
            Long excludedDogId
    ) {
        return !this.dogRepository.findPotentialDuplicateIds(
                centerId,
                tutorId,
                name,
                excludedDogId
        ).isEmpty();
    }

    private TutorEntity findTutor(Long tutorId, Long centerId) {
        if (tutorId == null || tutorId < 0) {
            throw new IllegalArgumentException("Debes seleccionar un tutor válido");
        }
        return this.tutorRepository.findByIdAndCenter_Id(tutorId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el tutor indicado"));
    }

    private void validateTutorCenter(DogEntity dog) {
        TutorEntity tutor = dog.getTutor();
        if (tutor != null && !tutor.getCenter().getId().equals(dog.getCenter().getId())) {
            throw new ResourceNotFoundException("No existe el perro indicado");
        }
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

    private DogDto mapToDto(DogEntity dog) {
        TutorEntity tutor = dog.getTutor();
        String tutorName = tutor == null
                ? null
                : tutor.getFirstName() + " " + tutor.getLastName();
        return new DogDto(
                dog.getId(),
                dog.getName(),
                dog.getBreed(),
                dog.getSize().name(),
                dog.getBirthDate(),
                tutor == null ? null : tutor.getId(),
                tutorName,
                tutor == null ? null : tutor.getNationalId()
        );
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String updatedBreed(DogUpdateRequestDto request, DogEntity dog) {
        if (request.isClearBreed()) {
            if (request.getBreed() != null && !request.getBreed().isBlank()) {
                throw new IllegalArgumentException("No se puede indicar una raza y vaciarla a la vez");
            }
            return null;
        }
        if (request.getBreed() == null || request.getBreed().isBlank()) {
            return dog.getBreed();
        }
        return normalizeOptionalText(request.getBreed());
    }

    private LocalDate updatedBirthDate(DogUpdateRequestDto request, DogEntity dog) {
        if (request.isClearBirthDate()) {
            if (request.getBirthDate() != null) {
                throw new IllegalArgumentException(
                        "No se puede indicar una fecha de nacimiento y vaciarla a la vez"
                );
            }
            return null;
        }
        return request.getBirthDate() == null ? dog.getBirthDate() : request.getBirthDate();
    }

    private DogSize parseSize(String size) {
        if (size == null || size.isBlank()) {
            throw new IllegalArgumentException("El tamaño es obligatorio");
        }
        try {
            return DogSize.valueOf(size.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El tamaño no es válido", exception);
        }
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now(this.receptionClock))) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
    }

    private void validateBreedLength(String breed) {
        if (breed != null && breed.length() > 120) {
            throw new IllegalArgumentException("La raza no puede superar 120 caracteres");
        }
    }

    private void validateId(Long id) {
        if (id == null || id < 0) {
            throw new IllegalArgumentException("El identificador del perro no puede ser negativo");
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

}
