package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.IncompatibilityEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import com.campusdual.cd2026mnd1g1.model.repository.DogRepository;
import com.campusdual.cd2026mnd1g1.model.repository.IncompatibilityRepository;
import com.campusdual.cd2026mnd1g1.model.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncompatibilityServiceTest {

    @Mock
    private DogRepository dogRepository;

    @Mock
    private IncompatibilityRepository incompatibilityRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private IncompatibilityService incompatibilityService;

    @Test
    void createStoresDogIdsInAscendingOrder() {
        DogEntity dogFour = dog(4L);
        DogEntity dogNine = dog(9L);
        when(this.dogRepository.findById(4L)).thenReturn(Optional.of(dogFour));
        when(this.dogRepository.findById(9L)).thenReturn(Optional.of(dogNine));
        when(this.incompatibilityRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        this.incompatibilityService.create(9L, 4L, "Conflicto");

        ArgumentCaptor<IncompatibilityEntity> captor = ArgumentCaptor.forClass(IncompatibilityEntity.class);
        verify(this.incompatibilityRepository).save(captor.capture());
        assertEquals(4L, captor.getValue().getFirstDog().getId());
        assertEquals(9L, captor.getValue().getSecondDog().getId());
    }

    @Test
    void findsIncompatibleDogsUsingAscendingIds() {
        LocalDate date = LocalDate.now().plusDays(1);
        ReservationEntity existingReservation = new ReservationEntity();
        existingReservation.setDog(dog(9L));
        when(this.reservationRepository.findByDateAndGroup_IdAndStatusNot(
                date, 3L, ReservationStatus.CANCELADA
        )).thenReturn(List.of(existingReservation));
        when(this.incompatibilityRepository.existsBetweenDogs(4L, 9L)).thenReturn(true);

        List<Long> conflicts = this.incompatibilityService.findConflictingDogIds(4L, 3L, date);

        assertEquals(List.of(9L), conflicts);
        verify(this.incompatibilityRepository).existsBetweenDogs(4L, 9L);
    }

    private DogEntity dog(Long id) {
        DogEntity dog = new DogEntity();
        dog.setId(id);
        return dog;
    }
}
