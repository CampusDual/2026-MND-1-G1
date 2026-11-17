package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.model.entity.IncidentEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.repository.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void createRejectsIncidentWithoutReservationOrAppointment() {
        IncidentEntity incident = new IncidentEntity();

        assertThrows(IllegalArgumentException.class, () -> this.incidentService.create(incident));

        verify(this.incidentRepository, never()).save(any());
    }

    @Test
    void createAcceptsIncidentLinkedToReservation() {
        IncidentEntity incident = new IncidentEntity();
        incident.setReservation(new ReservationEntity());
        when(this.incidentRepository.save(incident)).thenReturn(incident);

        this.incidentService.create(incident);

        verify(this.incidentRepository).save(incident);
    }

    @Test
    void createAcceptsIncidentLinkedToAppointmentId() {
        IncidentEntity incident = new IncidentEntity();
        incident.setAppointmentId(25L);
        when(this.incidentRepository.save(incident)).thenReturn(incident);

        this.incidentService.create(incident);

        verify(this.incidentRepository).save(incident);
    }
}
