package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestOriginDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestStatusDto;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionGroomingRequestService;
import com.campusdual.cd2026mnd1g1.model.entity.CenterType;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingRequestEntity;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingRequestStatus;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.OperationalUserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.GroomingRequestTransitionConflictException;
import com.campusdual.cd2026mnd1g1.model.repository.GroomingRequestRepository;
import com.campusdual.cd2026mnd1g1.model.repository.GroomingRequestServiceNameProjection;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta solicitudes de peluquería del centro del usuario autenticado.
 */
@Service
@Transactional(readOnly = true)
public class ReceptionGroomingRequestServiceImpl implements ReceptionGroomingRequestService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final GroomingRequestRepository groomingRequestRepository;

    public ReceptionGroomingRequestServiceImpl(
            UserRepository userRepository,
            GroomingRequestRepository groomingRequestRepository
    ) {
        this.userRepository = userRepository;
        this.groomingRequestRepository = groomingRequestRepository;
    }

    @Override
    public GroomingRequestPageDto findRequests(
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
        CenterType originCenterType = toCenterType(origin);
        Page<GroomingRequestEntity> requestPage = this.groomingRequestRepository.findVisibleRequests(
                user.getCenter().getId(),
                originCenterType,
                toStatus(status),
                fromDate,
                toDate,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Order.desc("requestedDate"), Sort.Order.desc("id"))
                )
        );
        Map<Long, List<String>> serviceNames = findServiceNames(requestPage.getContent(), user.getCenter().getId());
        List<GroomingRequestDto> items = requestPage.getContent().stream()
                .map(request -> toDto(request, serviceNames.getOrDefault(request.getId(), List.of())))
                .toList();

        return new GroomingRequestPageDto(
                items,
                requestPage.getNumber(),
                requestPage.getSize(),
                requestPage.getTotalElements(),
                requestPage.getTotalPages()
        );
    }

    @Override
    public GroomingRequestDetailDto findRequest(Long requestId, String username) {
        if (requestId == null || requestId < 0) {
            throw new IllegalArgumentException("El identificador de solicitud no puede ser negativo");
        }
        UserEntity user = findActiveReceptionist(username);
        GroomingRequestEntity request = this.groomingRequestRepository.findVisibleRequest(
                        requestId,
                        user.getCenter().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("No existe la solicitud indicada"));
        List<String> services = findServiceNames(List.of(request), user.getCenter().getId())
                .getOrDefault(request.getId(), List.of());

        return new GroomingRequestDetailDto(
                request.getId(),
                request.getDog().getName(),
                customerName(request),
                services,
                request.getRequestedDate(),
                request.getRequestedTime(),
                GroomingRequestStatusDto.valueOf(request.getStatus().name()),
                toOrigin(request),
                request.getNotes()
        );
    }

    @Override
    @Transactional
    public GroomingRequestDetailDto confirmRequest(Long requestId, String username) {
        return transitionPendingRequest(requestId, username, GroomingRequestStatus.CONFIRMADA);
    }

    @Override
    @Transactional
    public GroomingRequestDetailDto rejectRequest(Long requestId, String username) {
        return transitionPendingRequest(requestId, username, GroomingRequestStatus.RECHAZADA);
    }

    private GroomingRequestDetailDto transitionPendingRequest(
            Long requestId,
            String username,
            GroomingRequestStatus targetStatus
    ) {
        if (requestId == null || requestId < 0) {
            throw new IllegalArgumentException("El identificador de solicitud no puede ser negativo");
        }
        UserEntity user = findActiveReceptionist(username);
        GroomingRequestEntity request = this.groomingRequestRepository.findVisibleRequestForUpdate(
                        requestId,
                        user.getCenter().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("No existe la solicitud indicada"));
        if (request.getStatus() != GroomingRequestStatus.SOLICITADA) {
            throw new GroomingRequestTransitionConflictException(
                    "Solo se pueden confirmar o rechazar solicitudes pendientes"
            );
        }
        request.setStatus(targetStatus);
        this.groomingRequestRepository.saveAndFlush(request);
        List<String> services = findServiceNames(List.of(request), user.getCenter().getId())
                .getOrDefault(request.getId(), List.of());
        return new GroomingRequestDetailDto(
                request.getId(),
                request.getDog().getName(),
                customerName(request),
                services,
                request.getRequestedDate(),
                request.getRequestedTime(),
                GroomingRequestStatusDto.valueOf(request.getStatus().name()),
                toOrigin(request),
                request.getNotes()
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

    private Map<Long, List<String>> findServiceNames(List<GroomingRequestEntity> requests, Long serviceCenterId) {
        if (requests.isEmpty()) {
            return Map.of();
        }
        List<Long> requestIds = requests.stream().map(GroomingRequestEntity::getId).toList();
        Map<Long, List<String>> namesByRequest = new LinkedHashMap<>();
        for (GroomingRequestServiceNameProjection service : this.groomingRequestRepository
                .findServiceNames(requestIds, serviceCenterId)) {
            namesByRequest.computeIfAbsent(service.getRequestId(), ignored -> new ArrayList<>())
                    .add(service.getServiceName());
        }
        return namesByRequest;
    }

    private GroomingRequestDto toDto(GroomingRequestEntity request, List<String> serviceNames) {
        return new GroomingRequestDto(
                request.getId(),
                request.getDog().getName(),
                customerName(request),
                serviceNames,
                request.getRequestedDate(),
                request.getRequestedTime(),
                GroomingRequestStatusDto.valueOf(request.getStatus().name()),
                toOrigin(request)
        );
    }

    private String customerName(GroomingRequestEntity request) {
        if (request.getDog().getCenter().getType() == CenterType.CLIENTE) {
            return request.getCustomerCenter().getName();
        }
        if (request.getDog().getTutor() == null) {
            return null;
        }
        return fullName(request.getDog().getTutor().getFirstName(), request.getDog().getTutor().getLastName());
    }

    private String fullName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }

    private GroomingRequestOriginDto toOrigin(GroomingRequestEntity request) {
        return switch (request.getDog().getCenter().getType()) {
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
