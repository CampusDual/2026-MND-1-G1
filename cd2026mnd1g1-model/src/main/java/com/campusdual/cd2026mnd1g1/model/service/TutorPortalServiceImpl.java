package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.TutorDogDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogAllergyInputDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogAuthorizedPersonInputDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogCreateRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogCreateResultDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogMedicationInputDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogSummaryDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorAuthorizedPersonDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorMedicationDto;
import com.campusdual.cd2026mnd1g1.api.service.TutorPortalService;
import com.campusdual.cd2026mnd1g1.model.entity.AllergyEntity;
import com.campusdual.cd2026mnd1g1.model.entity.AuthorizedPersonEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DogSize;
import com.campusdual.cd2026mnd1g1.model.entity.MedicationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.TutorDogSubmissionEntity;
import com.campusdual.cd2026mnd1g1.model.entity.TutorEntity;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.DuplicateTutorDogSubmissionException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.TutorAccountInconsistentException;
import com.campusdual.cd2026mnd1g1.model.exception.TutorAccountNotLinkedException;
import com.campusdual.cd2026mnd1g1.model.repository.AllergyRepository;
import com.campusdual.cd2026mnd1g1.model.repository.AuthorizedPersonRepository;
import com.campusdual.cd2026mnd1g1.model.repository.DogRepository;
import com.campusdual.cd2026mnd1g1.model.repository.MedicationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.TutorDogSubmissionRepository;
import com.campusdual.cd2026mnd1g1.model.repository.TutorRepository;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class TutorPortalServiceImpl implements TutorPortalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TutorPortalServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final DogRepository dogRepository;
    private final AllergyRepository allergyRepository;
    private final MedicationRepository medicationRepository;
    private final AuthorizedPersonRepository authorizedPersonRepository;
    private final TutorRepository tutorRepository;
    private final TutorDogSubmissionRepository submissionRepository;
    private final Clock receptionClock;

    public TutorPortalServiceImpl(
            UserRepository userRepository,
            DogRepository dogRepository,
            AllergyRepository allergyRepository,
            MedicationRepository medicationRepository,
            AuthorizedPersonRepository authorizedPersonRepository,
            TutorRepository tutorRepository,
            TutorDogSubmissionRepository submissionRepository,
            Clock receptionClock
    ) {
        this.userRepository = userRepository;
        this.dogRepository = dogRepository;
        this.allergyRepository = allergyRepository;
        this.medicationRepository = medicationRepository;
        this.authorizedPersonRepository = authorizedPersonRepository;
        this.tutorRepository = tutorRepository;
        this.submissionRepository = submissionRepository;
        this.receptionClock = receptionClock;
    }

    @Override
    public TutorDogPageDto findMyDogs(String username, int page, int size) {
        validatePaging(page, size);
        UserEntity account = findLinkedAccount(username);
        Page<DogEntity> dogs = this.dogRepository.findTutorPortalDogs(
                account.getTutor().getId(),
                account.getCenter().getId(),
                PageRequest.of(page, size, Sort.by(
                        Sort.Order.asc("name").ignoreCase(),
                        Sort.Order.asc("id")
                ))
        );
        return new TutorDogPageDto(
                dogs.getContent().stream()
                        .map(dog -> new TutorDogSummaryDto(
                                dog.getId(),
                                dog.getName(),
                                dog.getBreed(),
                                dog.getSize().name(),
                                dog.getPhotoUrl() != null && !dog.getPhotoUrl().isBlank()
                        ))
                        .toList(),
                dogs.getNumber(),
                dogs.getSize(),
                dogs.getTotalElements(),
                dogs.getTotalPages()
        );
    }

    @Override
    public TutorDogDetailDto findMyDog(Long dogId, String username) {
        DogEntity dog = findOwnedDog(dogId, username);
        return mapDetail(dog);
    }

    @Override
    public String findMyDogPhotoReference(Long dogId, String username) {
        DogEntity dog = findOwnedDog(dogId, username);
        if (dog.getPhotoUrl() == null || dog.getPhotoUrl().isBlank()) {
            throw new ResourceNotFoundException("No hay una foto disponible para este perro");
        }
        return dog.getPhotoUrl();
    }

    @Override
    public List<TutorAuthorizedPersonDto> findMyAuthorizedPersons(String username) {
        UserEntity account = findLinkedAccount(username);
        return this.authorizedPersonRepository
                .findByTutor_IdOrderByNameAscIdAsc(account.getTutor().getId())
                .stream()
                .map(person -> new TutorAuthorizedPersonDto(person.getName(), person.getPhone()))
                .toList();
    }

    @Override
    @Transactional
    public TutorDogCreateResultDto createMyDog(
            TutorDogCreateRequestDto request,
            String username,
            String idempotencyKey,
            String payloadHash,
            String photoReference
    ) {
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la petición no puede ser nulo");
        }
        UserEntity account = findLinkedAccount(username);
        TutorEntity tutor = this.tutorRepository.findByIdForUpdate(account.getTutor().getId())
                .orElseThrow(TutorAccountInconsistentException::new);
        if (!account.getCenter().getId().equals(tutor.getCenter().getId())) {
            throw new TutorAccountInconsistentException();
        }

        TutorDogSubmissionEntity previous = this.submissionRepository
                .findByTutor_IdAndIdempotencyKey(tutor.getId(), idempotencyKey)
                .orElse(null);
        if (previous != null) {
            if (!previous.getPayloadHash().equals(payloadHash)) {
                throw new DuplicateTutorDogSubmissionException(
                        "La clave de reintento ya se utilizó con otros datos. Inicia una nueva alta."
                );
            }
            return new TutorDogCreateResultDto(mapDetail(previous.getDog()), true);
        }

        LocalDate today = LocalDate.now(this.receptionClock);
        if (request.birthDate() != null && request.birthDate().isAfter(today)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
        String name = requiredText(request.name(), "El nombre del perro es obligatorio");
        String breed = optionalText(request.breed());
        String feedingInstructions = optionalText(request.feedingInstructions());
        String temperament = optionalText(request.temperament());
        DogSize size = parseSize(request.size());

        DogEntity dog = new DogEntity();
        dog.setCenter(account.getCenter());
        dog.setTutor(tutor);
        dog.setName(name);
        dog.setBreed(breed);
        dog.setSize(size);
        dog.setBirthDate(request.birthDate());
        dog.setFeedingInstructions(feedingInstructions);
        dog.setTemperament(temperament);
        dog.setPhotoUrl(photoReference);
        dog.setActive(true);
        dog.setInformationDeclaredByTutor(true);
        dog.setCreatedAt(LocalDateTime.now(this.receptionClock));
        DogEntity savedDog = this.dogRepository.saveAndFlush(dog);

        for (TutorDogAllergyInputDto input : safeList(request.allergies())) {
            AllergyEntity allergy = new AllergyEntity();
            allergy.setDog(savedDog);
            allergy.setDescription(requiredText(
                    input == null ? null : input.description(),
                    "La descripción de cada alergia es obligatoria"
            ));
            this.allergyRepository.save(allergy);
        }
        for (TutorDogMedicationInputDto input : safeList(request.medications())) {
            MedicationEntity medication = new MedicationEntity();
            medication.setDog(savedDog);
            medication.setMedication(requiredText(
                    input == null ? null : input.medication(),
                    "El nombre del medicamento es obligatorio"
            ));
            medication.setDosage(optionalText(input == null ? null : input.dosage()));
            medication.setInstructions(requiredText(
                    input == null ? null : input.instructions(),
                    "La pauta del medicamento es obligatoria"
            ));
            this.medicationRepository.save(medication);
        }
        for (TutorDogAuthorizedPersonInputDto input : safeList(request.authorizedPersons())) {
            String personName = optionalText(input == null ? null : input.name());
            String phone = optionalText(input == null ? null : input.phone());
            if (personName == null && phone == null) {
                continue;
            }
            if (personName == null || phone == null) {
                throw new IllegalArgumentException(
                        "Cada persona autorizada debe incluir nombre y teléfono, o dejarse completamente vacía"
                );
            }
            AuthorizedPersonEntity person = new AuthorizedPersonEntity();
            person.setTutor(tutor);
            person.setName(personName);
            person.setPhone(phone);
            person.setCreatedBy(account);
            person.setCreatedAt(LocalDateTime.now(this.receptionClock));
            this.authorizedPersonRepository.save(person);
        }

        TutorDogSubmissionEntity submission = new TutorDogSubmissionEntity();
        submission.setTutor(tutor);
        submission.setIdempotencyKey(idempotencyKey);
        submission.setPayloadHash(payloadHash);
        submission.setDog(savedDog);
        submission.setCreatedAt(LocalDateTime.now(this.receptionClock));
        this.submissionRepository.save(submission);
        return new TutorDogCreateResultDto(mapDetail(savedDog), false);
    }

    private UserEntity findLinkedAccount(String username) {
        UserEntity account = this.userRepository.findPortalAccountByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("No se ha encontrado la cuenta autenticada"));
        if (account.getTutor() == null) {
            throw new TutorAccountNotLinkedException();
        }
        if (account.getCenter() == null
                || account.getTutor().getCenter() == null
                || !account.getCenter().getId().equals(account.getTutor().getCenter().getId())) {
            LOGGER.error("Incoherencia de tutor/centro en la cuenta autenticada con id {}", account.getId());
            throw new TutorAccountInconsistentException();
        }
        return account;
    }

    private DogEntity findOwnedDog(Long dogId, String username) {
        if (dogId == null || dogId < 0) {
            throw new IllegalArgumentException("El identificador del perro no puede ser negativo");
        }
        UserEntity account = findLinkedAccount(username);
        return this.dogRepository.findTutorPortalDog(
                        dogId,
                        account.getTutor().getId(),
                        account.getCenter().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Perro no encontrado"));
    }

    private TutorDogDetailDto mapDetail(DogEntity dog) {
        List<String> allergies = this.allergyRepository.findByDog_IdOrderByIdAsc(dog.getId())
                .stream()
                .map(allergy -> allergy.getDescription())
                .toList();
        List<TutorMedicationDto> medications = this.medicationRepository
                .findByDog_IdOrderByIdAsc(dog.getId())
                .stream()
                .map(medication -> new TutorMedicationDto(
                        medication.getMedication(),
                        medication.getDosage(),
                        medication.getInstructions()
                ))
                .toList();
        return new TutorDogDetailDto(
                dog.getId(),
                dog.getName(),
                dog.getBreed(),
                dog.getSize().name(),
                dog.getBirthDate(),
                dog.getPhotoUrl() != null && !dog.getPhotoUrl().isBlank(),
                dog.getFeedingInstructions(),
                dog.getTemperament(),
                allergies,
                medications,
                this.authorizedPersonRepository
                        .findByTutor_IdOrderByNameAscIdAsc(dog.getTutor().getId())
                        .stream()
                        .map(person -> new TutorAuthorizedPersonDto(person.getName(), person.getPhone()))
                        .toList()
        );
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private DogSize parseSize(String size) {
        try {
            return DogSize.valueOf(requiredText(size, "El tamaño es obligatorio").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El tamaño no es válido", exception);
        }
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void validatePaging(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("La página debe ser positiva y el tamaño estar entre 1 y 100");
        }
    }
}
