import { ActivatedRoute, ParamMap, Router, convertToParamMap } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import { ReceptionReservationPage } from '../core/operational.models';
import { ItWorksComponent } from './it-works.component';

describe('ItWorksComponent', () => {
  let fixture: ComponentFixture<ItWorksComponent>;
  let routeParameters: Subject<ParamMap>;
  let daycareService: jasmine.SpyObj<DaycareOperationsService>;

  beforeEach(async () => {
    routeParameters = new Subject<ParamMap>();
    daycareService = jasmine.createSpyObj<DaycareOperationsService>(
      'DaycareOperationsService',
      ['getReceptionReservations']
    );

    await TestBed.configureTestingModule({
      imports: [ItWorksComponent],
      providers: [
        { provide: AuthService, useValue: { logout: jasmine.createSpy('logout') } },
        { provide: DaycareOperationsService, useValue: daycareService },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: routeParameters.asObservable(),
            snapshot: { queryParamMap: convertToParamMap({}) }
          }
        },
        {
          provide: Router,
          useValue: {
            navigate: jasmine.createSpy('navigate').and.resolveTo(true),
            url: '/tablero-diario'
          }
        }
      ]
    }).overrideComponent(ItWorksComponent, { set: { template: '', imports: [] } }).compileComponents();
  });

  it('does not let a previous date response replace the latest selection', () => {
    const oldResponse: Subject<ReceptionReservationPage> = new Subject();
    const currentResponse: Subject<ReceptionReservationPage> = new Subject();
    daycareService.getReceptionReservations.and.returnValues(oldResponse, currentResponse);
    fixture = TestBed.createComponent(ItWorksComponent);
    fixture.detectChanges();

    routeParameters.next(convertToParamMap({ date: '2026-11-17' }));
    routeParameters.next(convertToParamMap({ date: '2026-11-18' }));
    oldResponse.next(pageWithDog('Fecha antigua'));
    currentResponse.next(pageWithDog('Fecha seleccionada'));

    expect(fixture.componentInstance.reservations[0].dogName).toBe('Fecha seleccionada');
  });

  it('requests the selected date from the backend', () => {
    daycareService.getReceptionReservations.and.returnValue(of(emptyPage()));
    fixture = TestBed.createComponent(ItWorksComponent);
    fixture.detectChanges();
    routeParameters.next(convertToParamMap({ date: '2026-11-19' }));

    expect(daycareService.getReceptionReservations).toHaveBeenCalledWith('2026-11-19', 0, 50);
  });
});

function pageWithDog(dogName: string): ReceptionReservationPage {
  return {
    items: [{
      id: 1,
      date: '2026-11-18',
      modality: 'DIA_COMPLETO',
      status: 'CONFIRMADA',
      dogId: 2,
      dogName,
      tutorName: 'Ana Núñez',
      presenceStatus: 'SIN_ENTRADA_REGISTRADA'
    }],
    page: 0,
    size: 50,
    totalElements: 1,
    totalPages: 1
  };
}

function emptyPage(): ReceptionReservationPage {
  return { items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
}
