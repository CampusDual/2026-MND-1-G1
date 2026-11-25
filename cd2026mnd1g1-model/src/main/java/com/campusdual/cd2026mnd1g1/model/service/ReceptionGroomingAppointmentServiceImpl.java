package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestOriginDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestStatusDto;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionGroomingAppointmentService;
import com.campusdual.cd2026mnd1g1.model.entity.CenterType;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingAppointmentEntity;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingRequestStatus;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.OperationalUserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.repository.GroomingAppointmentRepository;
import com.campusdual.cd2026mnd1g1.model.repository.GroomingAppointmentServiceNameProjection;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta citas de peluquería dentro del centro del usuario autenticado.
 */
@Service
@Transactional(readOnly = true)
public class ReceptionGroomingAppointmentServiceImpl implements ReceptionGroomingAppointmentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final GroomingAppointmentRepository appointmentRepository;

    public ReceptionGroomingAppointmentServiceImpl(
            UserRepository userRepository,
            GroomingAppointmentRepository appointmentRepository
    ) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public GroomingAppointmentPageDto findAppointments(
            GroomingRequestOriginDto origin,
            GroomingRequestStatusDto status,
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
                        user.getCenter().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cita indicada"));
        List<String> services = findServiceNames(List.of(appointment), user.getCenter().getId())
                .getOrDefault(appointment.getId(), List.of());
        return new GroomingAppointmentDetailDto(
                appointment.getId(),
                appointment.getDog().getName(),
                customerName(appointment),
                services,
                appointment.getStartAt().toLocalDate(),
                appointment.getStartAt().toLocalTime(),
                appointment.getDurationMinutes(),
                appointment.getLocation(),
                GroomingRequestStatusDto.valueOf(appointment.getStatus().name()),
                toOrigin(appointment),
                visibleRequestNotes(appointment)
        );
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
                GroomingRequestStatusDto.valueOf(appointment.getStatus().name()),
                toOrigin(appointment)
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

    private String visibleRequestNotes(GroomingAppointmentEntity appointment) {
        if (appointment.getRequest() == null
                || appointment.getRequest().getDog() == null
                || !appointment.getRequest().getDog().getId().equals(appointment.getDog().getId())
                || !appointment.getRequest().getCustomerCenter().getId()
                        .equals(appointment.getDog().getCenter().getId())) {
            return null;
        }
        return appointment.getRequest().getNotes();
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

    private GroomingRequestStatus toStatus(GroomingRequestStatusDto status) {
        return status == null ? null : GroomingRequestStatus.valueOf(status.name());
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
