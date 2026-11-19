import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import { ReceptionReservationDetail } from '../core/operational.models';
import { ReceptionArrivalComponent } from './reception-arrival.component';

describe('ReceptionArrivalComponent', () => {
  let fixture: ComponentFixture<ReceptionArrivalComponent>;
  let router: jasmine.SpyObj<Router>;
  let daycareService: jasmine.SpyObj<DaycareOperationsService>;

  beforeEach(async () => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    daycareService = jasmine.createSpyObj<DaycareOperationsService>(
      'DaycareOperationsService',
      ['getReceptionReservation', 'registerReceptionArrival']
    );
    daycareService.getReceptionReservation.and.returnValue(of(reservationForToday()));
    daycareService.registerReceptionArrival.and.returnValue(of({
      id: 20,
      reservationId: 12,
      type: 'ENTRADA',
      dateTime: '2026-11-19T10:00:00',
      observations: 'Mochila y correa',
      reservationStatus: 'EN_CURSO'
    }));

    await TestBed.configureTestingModule({
      imports: [ReceptionArrivalComponent],
      providers: [
        { provide: AuthService, useValue: { logout: jasmine.createSpy('logout') } },
        { provide: DaycareOperationsService, useValue: daycareService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ date: todayInMadrid() }),
              paramMap: convertToParamMap({ id: '12' })
            }
          }
        },
        { provide: Router, useValue: router }
      ]
    }).overrideComponent(ReceptionArrivalComponent, { set: { template: '', imports: [] } }).compileComponents();
  });

  it('submits only observations and returns to detail after persistence', () => {
    fixture = TestBed.createComponent(ReceptionArrivalComponent);
    fixture.detectChanges();
    fixture.componentInstance.observations = 'Mochila y correa';

    fixture.componentInstance.submit();

    expect(daycareService.registerReceptionArrival).toHaveBeenCalledWith(12, {
      observations: 'Mochila y correa'
    });
    expect(router.navigate).toHaveBeenCalledWith(
      ['/tablero-diario/reservas', 12],
      { queryParams: { date: todayInMadrid() } }
    );
  });
});

function reservationForToday(): ReceptionReservationDetail {
  return {
    id: 12,
    date: todayInMadrid(),
    modality: 'DIA_COMPLETO',
    status: 'CONFIRMADA',
    dog: {
      id: 4,
      name: 'Luna',
      breed: null,
      size: 'MEDIANO',
      birthDate: null,
      temperament: null,
      feedingInstructions: null,
      allergies: [],
      medications: []
    },
    tutor: { name: 'Ana Núñez', phone: '981000101' },
    presence: {
      status: 'SIN_ENTRADA_REGISTRADA',
      lastEntryAt: null,
      entryPetCondition: null,
      entryObservations: null,
      entryResponsible: null,
      lastExitAt: null,
      exitResponsible: null,
      collectorType: null,
      collectorName: null,
      exitObservations: null,
      inconsistent: false
    }
  };
}

function todayInMadrid(): string {
  const parts: Intl.DateTimeFormatPart[] = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Madrid',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(new Date());
  const values: Map<string, string> = new Map(parts.map((part) => [part.type, part.value]));
  return `${values.get('year')}-${values.get('month')}-${values.get('day')}`;
}
