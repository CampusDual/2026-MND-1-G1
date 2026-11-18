import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  DogPage,
  ReceptionAvailability,
  ReceptionReservationCreated
} from '../core/operational.models';
import { ReservationCreateComponent } from './reservation-create.component';

describe('ReservationCreateComponent', () => {
  let fixture: ComponentFixture<ReservationCreateComponent>;
  let daycareService: jasmine.SpyObj<DaycareOperationsService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    daycareService = jasmine.createSpyObj<DaycareOperationsService>(
      'DaycareOperationsService',
      [
        'getReceptionGroups',
        'getReservationDogs',
        'getReceptionAvailability',
        'createReceptionReservation'
      ]
    );
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);
    daycareService.getReceptionGroups.and.returnValue(of([
      { id: 7, name: 'Grupo A', capacityPerSlot: 3 }
    ]));
    daycareService.getReservationDogs.and.returnValue(of(emptyDogPage()));

    await TestBed.configureTestingModule({
      imports: [ReservationCreateComponent],
      providers: [
        { provide: AuthService, useValue: { logout: jasmine.createSpy('logout') } },
        { provide: DaycareOperationsService, useValue: daycareService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({}) } }
        },
        { provide: Router, useValue: router }
      ]
    }).overrideComponent(ReservationCreateComponent, {
      set: { template: '', imports: [] }
    }).compileComponents();
  });

  it('does not let an older availability response replace the current date', () => {
    const oldResponse: Subject<ReceptionAvailability> = new Subject();
    const currentResponse: Subject<ReceptionAvailability> = new Subject();
    daycareService.getReceptionAvailability.and.returnValues(oldResponse, currentResponse);
    fixture = TestBed.createComponent(ReservationCreateComponent);
    fixture.detectChanges();
    const component: ReservationCreateComponent = fixture.componentInstance;
    const dates: string[] = nextTwoOpenDates(component.today);

    component.selectedGroupId = 7;
    component.selectedDate = dates[0];
    component.onBookingCriteriaChanged();
    component.selectedDate = dates[1];
    component.onBookingCriteriaChanged();
    oldResponse.next(availability(dates[0]));
    currentResponse.next(availability(dates[1]));

    expect(component.availability?.date).toBe(dates[1]);
  });

  it('opens the persisted reservation detail only after a successful response', () => {
    const createdResponse: Subject<ReceptionReservationCreated> = new Subject();
    daycareService.getReceptionAvailability.and.returnValue(of(availability('2099-01-05')));
    daycareService.createReceptionReservation.and.returnValue(createdResponse);
    fixture = TestBed.createComponent(ReservationCreateComponent);
    fixture.detectChanges();
    const component: ReservationCreateComponent = fixture.componentInstance;
    component.selectedDate = '2099-01-05';
    component.selectedDogId = 11;
    component.selectedDog = {
      id: 11,
      name: 'Luna',
      breed: null,
      size: 'MEDIANO',
      birthDate: null,
      tutorId: 5,
      tutorName: 'Ana García',
      tutorNationalId: 'TEST-1'
    };
    component.selectedGroupId = 7;
    component.availability = availability('2099-01-05');
    component.selectedModality = 'MANANA';

    component.saveReservation();
    expect(router.navigate).not.toHaveBeenCalled();

    createdResponse.next({
      id: 42,
      date: '2099-01-05',
      modality: 'MANANA',
      status: 'CONFIRMADA',
      dogId: 11,
      dogName: 'Luna',
      groupId: 7,
      groupName: 'Grupo A'
    });

    expect(router.navigate).toHaveBeenCalledWith(
      ['/tablero-diario/reservas', 42],
      { queryParams: { date: '2099-01-05' } }
    );
  });

  it('keeps the selected dog when a booking conflict is returned', () => {
    const dogPage: DogPage = {
      items: [{
        id: 11,
        name: 'Luna',
        breed: null,
        size: 'MEDIANO',
        birthDate: null,
        tutorId: 5,
        tutorName: 'Ana García',
        tutorNationalId: 'TEST-1'
      }],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1
    };
    daycareService.getReservationDogs.and.returnValue(of(dogPage));
    daycareService.getReceptionAvailability.and.returnValue(of(availability('2099-01-05')));
    daycareService.createReceptionReservation.and.returnValue(throwError(() =>
      new HttpErrorResponse({
        status: 409,
        error: { code: 'DUPLICATE_RESERVATION', message: 'El perro ya tiene una reserva activa.' }
      })
    ));
    fixture = TestBed.createComponent(ReservationCreateComponent);
    fixture.detectChanges();
    const component: ReservationCreateComponent = fixture.componentInstance;
    component.selectedDate = '2099-01-05';
    component.selectedDogId = 11;
    component.selectedDog = dogPage.items[0];
    component.selectedGroupId = 7;
    component.availability = availability('2099-01-05');
    component.selectedModality = 'MANANA';

    component.saveReservation();

    expect(component.selectedDogId).toBe(11);
    expect(component.bookingError).toContain('reserva activa');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});

function emptyDogPage(): DogPage {
  return { items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
}

function availability(date: string): ReceptionAvailability {
  return {
    date,
    groupId: 7,
    groupName: 'Grupo A',
    capacityPerSlot: 3,
    morningOccupied: 0,
    morningRemaining: 3,
    afternoonOccupied: 0,
    afternoonRemaining: 3
  };
}

function nextTwoOpenDates(today: string): string[] {
  const first: Date = new Date(`${today}T00:00:00Z`);
  while (first.getUTCDay() === 0) {
    first.setUTCDate(first.getUTCDate() + 1);
  }
  const second: Date = new Date(first);
  second.setUTCDate(second.getUTCDate() + 1);
  if (second.getUTCDay() === 0) {
    second.setUTCDate(second.getUTCDate() + 1);
  }
  return [first.toISOString().slice(0, 10), second.toISOString().slice(0, 10)];
}
