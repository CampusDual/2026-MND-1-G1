package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.AssignGroomingAppointmentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.CreateGroomingAppointmentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingDogOptionDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingDogOptionPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingServiceOptionDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestOriginDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentStatusDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomerAgendaDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomerOptionDto;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionGroomingAppointmentService;
import com.campusdual.cd2026mnd1g1.model.entity.CenterType;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingAppointmentEntity;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingAppointmentStatus;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingServiceEntity;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.GroomingAppointmentAssignmentConflictException;
import com.campusdual.cd2026mnd1g1.model.exception.OperationalUserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.repository.GroomingAppointmentRepository;
import com.campusdual.cd2026mnd1g1.model.repository.GroomingAppointmentServiceNameProjection;
import com.campusdual.cd2026mnd1g1.model.repository.DogRepository;
import com.campusdual.cd2026mnd1g1.model.repository.GroomingServiceRepository;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Consulta citas de peluquería dentro del centro del usuario autenticado.
 */
@Service
@Transactional(readOnly = true)
public class ReceptionGroomingAppointmentServiceImpl implements ReceptionGroomingAppointmentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final GroomingAppointmentRepository appointmentRepository;
    private final DogRepository dogRepository;
    private final GroomingServiceRepository serviceRepository;

    public ReceptionGroomingAppointmentServiceImpl(
            UserRepository userRepository,
            GroomingAppointmentRepository appointmentRepository,
            DogRepository dogRepository,
            GroomingServiceRepository serviceRepository
    ) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.dogRepository = dogRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public GroomingDogOptionPageDto findInternalDogs(String search, int page, int size, String username) {
        validatePagination(page, size);
        UserEntity user = findActiveReceptionist(username);
        validateReceptionCenter(user);
        Page<DogEntity> dogs = this.dogRepository.findInternalGroomingDogs(
                user.getCenter().getId(),
                search == null ? "" : search.trim(),
                PageRequest.of(page, size, Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id")))
        );
        List<GroomingDogOptionDto> items = dogs.getContent().stream()
                .map(dog -> new GroomingDogOptionDto(
                        dog.getId(),
                        dog.getName(),
                        dog.getTutor() == null
                                ? null
                                : dog.getTutor().getFirstName() + " " + dog.getTutor().getLastName()
                ))
                .toList();
        return new GroomingDogOptionPageDto(
                items,
                dogs.getNumber(),
                dogs.getSize(),
                dogs.getTotalElements(),
                dogs.getTotalPages()
        );
    }

    private String groomerName(GroomingAppointmentEntity appointment) {
        UserEntity groomer = appointment.getGroomer();
        if (groomer == null) {
            return null;
        }
        return groomer.getFirstName() + " " + groomer.getLastName();
    }

    @Override
    public List<GroomingServiceOptionDto> findAvailableServices(String username) {
        UserEntity user = findActiveReceptionist(username);
        validateReceptionCenter(user);
        return this.serviceRepository.findByCenter_IdAndActiveTrueOrderByNameAscIdAsc(user.getCenter().getId())
                .stream()
                .map(service -> new GroomingServiceOptionDto(service.getId(), service.getName()))
                .toList();
    }

    @Override
    @Transactional
    public GroomingAppointmentDetailDto createAppointment(
            CreateGroomingAppointmentRequestDto request,
            String username
    ) {
        validateCreateRequest(request);
        UserEntity user = findActiveReceptionist(username);
        validateReceptionCenter(user);

        Long centerId = user.getCenter().getId();
        DogEntity dog = this.dogRepository.findInternalGroomingDog(request.dogId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el perro interno indicado"
                ));
        GroomingServiceEntity groomingService = this.serviceRepository
                        .findByIdAndCenter_IdAndActiveTrue(request.serviceId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un servicio activo con ese identificador"
                ));

        GroomingAppointmentEntity appointment = new GroomingAppointmentEntity();
        appointment.setDog(dog);
        appointment.setCreatedBy(user);
        appointment.setStartAt(LocalDateTime.of(request.date(), request.time()));
        appointment.setDurationMinutes(groomingService.getDurationMinutes());
        appointment.setStatus(GroomingAppointmentStatus.PENDIENTE_ASIGNACION);
        appointment.setNotes(normalizeNotes(request.notes()));
        appointment.setServices(new LinkedHashSet<>(List.of(groomingService)));
        GroomingAppointmentEntity saved = this.appointmentRepository.saveAndFlush(appointment);
        return toDetailDto(saved, List.of(groomingService.getName()));
    }

    @Override
    public GroomingAppointmentPageDto findAppointments(
            GroomingRequestOriginDto origin,
            GroomingAppointmentStatusDto status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String username
    ) {
        validateFilters(fromDate, toDate, page, size);
        UserEntity user = findActiveReceptionist(username);
        Long centerId = user.getCenter().getId();
        Page<GroomingAppointmentEntity> appointmentPage = this.appointmentRepository.findVisibleAppointments(
                centerId,
                GroomingAppointmentStatus.PENDIENTE_ASIGNACION,
                CenterType.CLIENTE,
                toCenterType(origin),
                toStatus(status),
                fromDate == null ? null : fromDate.atStartOfDay(),
                toDate == null ? null : toDate.plusDays(1).atStartOfDay(),
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Order.asc("startAt"), Sort.Order.asc("id"))
                )
        );
        Map<Long, List<String>> serviceNames = findServiceNames(appointmentPage.getContent(), centerId);
        List<GroomingAppointmentDto> items = appointmentPage.getContent().stream()
                .map(appointment -> toDto(appointment, serviceNames.getOrDefault(appointment.getId(), List.of())))
                .toList();
        return new GroomingAppointmentPageDto(
                items,
                appointmentPage.getNumber(),
                appointmentPage.getSize(),
                appointmentPage.getTotalElements(),
                appointmentPage.getTotalPages()
        );
    }

    @Override
    public GroomingAppointmentDetailDto findAppointment(Long appointmentId, String username) {
        if (appointmentId == null || appointmentId < 0) {
            throw new IllegalArgumentException("El identificador de cita no puede ser negativo");
        }
        UserEntity user = findActiveReceptionist(username);
        GroomingAppointmentEntity appointment = this.appointmentRepository.findVisibleAppointment(
                        appointmentId,
                        user.getCenter().getId(),
                        GroomingAppointmentStatus.PENDIENTE_ASIGNACION,
                        CenterType.CLIENTE
                )
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cita indicada"));
        List<String> services = findServiceNames(List.of(appointment), user.getCenter().getId())
                .getOrDefault(appointment.getId(), List.of());
        return toDetailDto(appointment, services);
    }

    @Override
    @Transactional
    public GroomingAppointmentDetailDto assignAppointment(
            Long appointmentId,
            AssignGroomingAppointmentRequestDto request,
            String username
    ) {
        validateAssignmentRequest(appointmentId, request);
        UserEntity receptionist = findActiveReceptionist(username);
        validateReceptionCenter(receptionist);
        Long centerId = receptionist.getCenter().getId();
        GroomingAppointmentEntity appointment = this.appointmentRepository.findVisibleAppointmentForUpdate(
                        appointmentId,
                        centerId,
                        GroomingAppointmentStatus.PENDIENTE_ASIGNACION,
                        CenterType.CLIENTE
                )
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cita indicada"));
        if (appointment.getStatus() != GroomingAppointmentStatus.PENDIENTE_ASIGNACION) {
            throw new GroomingAppointmentAssignmentConflictException(
                    "La cita ya no está pendiente de asignación"
            );
        }
        UserEntity groomer = findActiveGroomer(request.groomerId(), centerId);
        appointment.setGroomer(groomer);
        appointment.setLocation(request.location().name());
        appointment.setStartAt(LocalDateTime.of(request.date(), request.time()));
        appointment.setStatus(GroomingAppointmentStatus.CONFIRMADA);
        GroomingAppointmentEntity saved = this.appointmentRepository.saveAndFlush(appointment);
        List<String> services = findServiceNames(List.of(saved), centerId)
                .getOrDefault(saved.getId(), List.of());
        return toDetailDto(saved, services);
    }

    @Override
    public List<GroomerOptionDto> findGroomers(String username) {
        UserEntity user = findActiveReceptionist(username);
        validateReceptionCenter(user);
        return this.userRepository
                .findByCenter_IdAndRole_NameAndActiveTrueOrderByFirstNameAscLastNameAscIdAsc(
                        user.getCenter().getId(),
                        "PELUQUERO"
                )
                .stream()
                .map(groomer -> new GroomerOptionDto(
                        groomer.getId(),
                        groomer.getFirstName() + " " + groomer.getLastName()
                ))
                .toList();
    }

    @Override
    public GroomerAgendaDto findGroomerAgenda(Long groomerId, LocalDate date, String username) {
        if (groomerId == null || groomerId < 0 || date == null) {
            throw new IllegalArgumentException("Peluquero y fecha son obligatorios");
        }
        UserEntity user = findActiveReceptionist(username);
        validateReceptionCenter(user);
        Long centerId = user.getCenter().getId();
        UserEntity groomer = findActiveGroomer(groomerId, centerId);
        List<GroomingAppointmentEntity> appointments = this.appointmentRepository.findGroomerAgenda(
                groomerId,
                centerId,
                Set.of(CenterType.PROPIA, CenterType.CLIENTE),
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                GroomingAppointmentStatus.PENDIENTE_ASIGNACION
        );
        Map<Long, List<String>> serviceNames = findServiceNames(appointments, centerId);
        List<GroomingAppointmentDto> items = appointments.stream()
                .map(appointment -> toDto(
                        appointment,
                        serviceNames.getOrDefault(appointment.getId(), List.of())
                ))
                .toList();
        return new GroomerAgendaDto(
                groomer.getId(),
                groomer.getFirstName() + " " + groomer.getLastName(),
                date,
                items
        );
    }

    @Override
    public GroomingAppointmentDetailDto findGroomerAgendaAppointment(
            Long groomerId,
            Long appointmentId,
            String username
    ) {
        if (groomerId == null || groomerId < 0 || appointmentId == null || appointmentId < 0) {
            throw new IllegalArgumentException("Los identificadores no pueden ser negativos");
        }
        UserEntity user = findActiveReceptionist(username);
        validateReceptionCenter(user);
        Long centerId = user.getCenter().getId();
        findActiveGroomer(groomerId, centerId);
        GroomingAppointmentEntity appointment = this.appointmentRepository.findGroomerAgendaAppointment(
                        appointmentId,
                        groomerId,
                        centerId,
                        GroomingAppointmentStatus.PENDIENTE_ASIGNACION
                )
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cita indicada en esta agenda"));
        List<String> services = findServiceNames(List.of(appointment), centerId)
                .getOrDefault(appointment.getId(), List.of());
        return toDetailDto(appointment, services);
    }

    private UserEntity findActiveGroomer(Long groomerId, Long centerId) {
        return this.userRepository
                .findByIdAndCenter_IdAndRole_NameAndActiveTrue(groomerId, centerId, "PELUQUERO")
                .orElseThrow(() -> new ResourceNotFoundException("No existe el peluquero indicado"));
    }

    private UserEntity findActiveReceptionist(String username) {
        UserEntity user = this.userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new OperationalUserNotFoundException(
                        "El usuario autenticado no tiene un centro asociado"
                ));
        if (!Boolean.TRUE.equals(user.getActive()) || user.getCenter() == null) {
            throw new OperationalUserNotFoundException("El usuario operativo está desactivado o sin centro");
        }
        return user;
    }

    private Map<Long, List<String>> findServiceNames(List<GroomingAppointmentEntity> appointments, Long centerId) {
        if (appointments.isEmpty()) {
            return Map.of();
        }
        List<Long> appointmentIds = appointments.stream().map(GroomingAppointmentEntity::getId).toList();
        Map<Long, List<String>> namesByAppointment = new LinkedHashMap<>();
        for (GroomingAppointmentServiceNameProjection service : this.appointmentRepository
                .findServiceNames(appointmentIds, centerId)) {
            namesByAppointment.computeIfAbsent(service.getAppointmentId(), ignored -> new ArrayList<>())
                    .add(service.getServiceName());
        }
        return namesByAppointment;
    }

    private GroomingAppointmentDto toDto(GroomingAppointmentEntity appointment, List<String> serviceNames) {
        return new GroomingAppointmentDto(
                appointment.getId(),
                appointment.getDog().getName(),
                customerName(appointment),
                serviceNames,
                appointment.getStartAt().toLocalDate(),
                appointment.getStartAt().toLocalTime(),
                appointment.getDurationMinutes(),
                appointment.getLocation(),
                groomerName(appointment),
                GroomingAppointmentStatusDto.valueOf(appointment.getStatus().name()),
                toOrigin(appointment)
        );
    }

    private GroomingAppointmentDetailDto toDetailDto(
            GroomingAppointmentEntity appointment,
            List<String> serviceNames
    ) {
        return new GroomingAppointmentDetailDto(
                appointment.getId(),
                appointment.getDog().getName(),
                customerName(appointment),
                serviceNames,
                appointment.getStartAt().toLocalDate(),
                appointment.getStartAt().toLocalTime(),
                appointment.getDurationMinutes(),
                appointment.getLocation(),
                groomerName(appointment),
                GroomingAppointmentStatusDto.valueOf(appointment.getStatus().name()),
                toOrigin(appointment),
                visibleAppointmentNotes(appointment)
        );
    }

    private String customerName(GroomingAppointmentEntity appointment) {
        if (appointment.getDog().getCenter().getType() == CenterType.CLIENTE) {
            return appointment.getDog().getCenter().getName();
        }
        if (appointment.getDog().getTutor() == null) {
            return null;
        }
        return appointment.getDog().getTutor().getFirstName() + " "
                + appointment.getDog().getTutor().getLastName();
    }

    private String visibleAppointmentNotes(GroomingAppointmentEntity appointment) {
        if (appointment.getNotes() != null) {
            return appointment.getNotes();
        }
        if (appointment.getRequest() == null
                || appointment.getRequest().getDog() == null
                || !appointment.getRequest().getDog().getId().equals(appointment.getDog().getId())
                || !appointment.getRequest().getCustomerCenter().getId()
                        .equals(appointment.getDog().getCenter().getId())) {
            return null;
        }
        return appointment.getRequest().getNotes();
    }

    private void validateReceptionCenter(UserEntity user) {
        if (user.getCenter().getType() != CenterType.PROPIA
                || !Boolean.TRUE.equals(user.getCenter().getActive())) {
            throw new OperationalUserNotFoundException(
                    "El usuario debe estar asociado a una guardería interna activa"
            );
        }
    }

    private void validateCreateRequest(CreateGroomingAppointmentRequestDto request) {
        if (request == null || request.dogId() == null || request.dogId() < 0
                || request.serviceId() == null || request.serviceId() < 0
                || request.date() == null || request.time() == null) {
            throw new IllegalArgumentException("Perro, servicio, fecha y hora son obligatorios");
        }
        if (request.notes() != null && request.notes().length() > 2000) {
            throw new IllegalArgumentException("Las observaciones no pueden superar 2000 caracteres");
        }
    }

    private void validateAssignmentRequest(
            Long appointmentId,
            AssignGroomingAppointmentRequestDto request
    ) {
        if (appointmentId == null || appointmentId < 0 || request == null
                || request.groomerId() == null || request.groomerId() < 0
                || request.location() == null || request.date() == null || request.time() == null) {
            throw new IllegalArgumentException("Cita, peluquero, lugar, fecha y hora son obligatorios");
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String normalized = notes.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("La paginación solicitada no es válida");
        }
    }

    private GroomingRequestOriginDto toOrigin(GroomingAppointmentEntity appointment) {
        return switch (appointment.getDog().getCenter().getType()) {
            case PROPIA -> GroomingRequestOriginDto.INTERNA;
            case CLIENTE -> GroomingRequestOriginDto.B2B;
        };
    }

    private CenterType toCenterType(GroomingRequestOriginDto origin) {
        if (origin == null) {
            return null;
        }
        return switch (origin) {
            case INTERNA -> CenterType.PROPIA;
            case B2B -> CenterType.CLIENTE;
        };
    }

    private GroomingAppointmentStatus toStatus(GroomingAppointmentStatusDto status) {
        return status == null ? null : GroomingAppointmentStatus.valueOf(status.name());
    }

    private void validateFilters(LocalDate fromDate, LocalDate toDate, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("La paginación solicitada no es válida");
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
    }
}
