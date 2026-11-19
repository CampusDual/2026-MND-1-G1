import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import { ReceptionDepartureData } from '../core/operational.models';
import { ReceptionDepartureComponent } from './reception-departure.component';

describe('ReceptionDepartureComponent', () => {
  let fixture: ComponentFixture<ReceptionDepartureComponent>;
  let router: jasmine.SpyObj<Router>;
  let daycareService: jasmine.SpyObj<DaycareOperationsService>;

  beforeEach(async () => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    daycareService = jasmine.createSpyObj<DaycareOperationsService>(
      'DaycareOperationsService',
      ['getReceptionDepartureData', 'registerReceptionDeparture']
    );
    daycareService.getReceptionDepartureData.and.returnValue(of(departureData()));
    daycareService.registerReceptionDeparture.and.returnValue(of({
      id: 23,
      reservationId: 12,
      type: 'SALIDA',
      dateTime: '2026-11-19T10:00:00',
      observations: 'Entrega confirmada',
      reservationStatus: 'COMPLETADA',
      collectorType: 'TUTOR',
      collectorName: 'Ana Núñez'
    }));

    await TestBed.configureTestingModule({
      imports: [ReceptionDepartureComponent],
      providers: [
        { provide: AuthService, useValue: { logout: jasmine.createSpy('logout') } },
        { provide: DaycareOperationsService, useValue: daycareService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ date: '2026-11-19' }),
              paramMap: convertToParamMap({ id: '12' })
            }
          }
        },
        { provide: Router, useValue: router }
      ]
    }).overrideComponent(ReceptionDepartureComponent, { set: { template: '', imports: [] } }).compileComponents();
  });

  it('allows registering a departure without observations', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    fixture = TestBed.createComponent(ReceptionDepartureComponent);
    fixture.detectChanges();

    fixture.componentInstance.submit();

    expect(daycareService.registerReceptionDeparture).toHaveBeenCalledWith(12, {
      collectorType: 'TUTOR',
      authorizedPersonId: null,
      observations: null
    });
  });

  it('submits only the server-validated collector and optional observations then returns to detail', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    fixture = TestBed.createComponent(ReceptionDepartureComponent);
    fixture.detectChanges();
    fixture.componentInstance.observations = '  Entrega confirmada  ';

    fixture.componentInstance.submit();

    expect(daycareService.registerReceptionDeparture).toHaveBeenCalledWith(12, {
      collectorType: 'TUTOR',
      authorizedPersonId: null,
      observations: 'Entrega confirmada'
    });
    expect(router.navigate).toHaveBeenCalledWith(
      ['/tablero-diario/reservas', 12],
      { queryParams: { date: '2026-11-19' } }
    );
  });
});

function departureData(): ReceptionDepartureData {
  return {
    reservationId: 12,
    date: '2026-11-19',
    modality: 'DIA_COMPLETO',
    reservationStatus: 'EN_CURSO',
    dogName: 'Luna',
    tutorId: 4,
    tutorName: 'Ana Núñez',
    entryAt: '2026-11-19T09:00:00',
    entryObservations: 'Arnés rojo',
    presenceStatus: 'EN_EL_CENTRO',
    authorizedPersons: [{ id: 31, name: 'María autorizada' }]
  };
}
