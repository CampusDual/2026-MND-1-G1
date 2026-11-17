package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.model.entity.IncidentEntity;
import com.campusdual.cd2026mnd1g1.model.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    /**
     * Guarda una incidencia si está asociada a una reserva o a una cita.
     *
     * @param incident incidencia que se va a guardar
     * @return incidencia persistida
     */
    public IncidentEntity create(IncidentEntity incident) {
        if (incident == null) {
            throw new IllegalArgumentException("La incidencia es obligatoria");
        }
        if (incident.getReservation() == null && incident.getAppointmentId() == null) {
            throw new IllegalArgumentException("La incidencia debe asociarse a una reserva o a una cita");
        }
        return this.incidentRepository.save(incident);
    }
}
