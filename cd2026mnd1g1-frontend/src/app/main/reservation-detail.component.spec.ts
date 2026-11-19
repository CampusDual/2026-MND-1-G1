import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import { ReceptionReservationDetail } from '../core/operational.models';
import { ReservationDetailComponent } from './reservation-detail.component';

describe('ReservationDetailComponent', () => {
  let fixture: ComponentFixture<ReservationDetailComponent>;
  let router: jasmine.SpyObj<Router>;
  let daycareService: jasmine.SpyObj<DaycareOperationsService>;

  beforeEach(async () => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    daycareService = jasmine.createSpyObj<DaycareOperationsService>(
      'DaycareOperationsService',
      ['getReceptionReservation']
    );
    const reservation: ReceptionReservationDetail = {
      id: 12,
      date: '2026-11-17',
      modality: 'MEDIA_JORNADA',
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
      tutor: null,
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
    daycareService.getReceptionReservation.and.returnValue(of(reservation));

    await TestBed.configureTestingModule({
      imports: [ReservationDetailComponent],
      providers: [
        { provide: AuthService, useValue: { logout: jasmine.createSpy('logout') } },
        { provide: DaycareOperationsService, useValue: daycareService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ date: '2026-11-17' }),
              paramMap: convertToParamMap({ id: '12' })
            }
          }
        },
        { provide: Router, useValue: router }
      ]
    }).overrideComponent(ReservationDetailComponent, { set: { template: '', imports: [] } }).compileComponents();
  });

  it('returns to the list with the previously selected date', () => {
    fixture = TestBed.createComponent(ReservationDetailComponent);
    fixture.detectChanges();
    fixture.componentInstance.backToList();

    expect(router.navigate).toHaveBeenCalledWith(
      ['/tablero-diario'],
      { queryParams: { date: '2026-11-17' } }
    );
  });
});
