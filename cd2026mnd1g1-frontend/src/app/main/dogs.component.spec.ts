import { convertToParamMap } from '@angular/router';
import { ActivatedRoute, Router } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import { DogPage, TutorPage } from '../core/operational.models';
import { DogsComponent } from './dogs.component';

describe('DogsComponent', () => {
  let fixture: ComponentFixture<DogsComponent>;
  let daycareService: jasmine.SpyObj<DaycareOperationsService>;
  let routeData: BehaviorSubject<Record<string, string>>;
  let routeParams: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  beforeEach(async () => {
    daycareService = jasmine.createSpyObj<DaycareOperationsService>(
      'DaycareOperationsService',
      ['getDogs', 'getTutors', 'getDog', 'createDog', 'updateDog']
    );
    daycareService.getDogs.and.returnValue(of(emptyDogPage()));
    daycareService.getTutors.and.returnValue(of(emptyTutorPage()));
    routeData = new BehaviorSubject<Record<string, string>>({ screen: 'list' });
    routeParams = new BehaviorSubject(convertToParamMap({ search: 'Luna', page: '0' }));

    await TestBed.configureTestingModule({
      imports: [DogsComponent],
      providers: [
        { provide: AuthService, useValue: { logout: jasmine.createSpy('logout') } },
        { provide: DaycareOperationsService, useValue: daycareService },
        {
          provide: ActivatedRoute,
          useValue: {
            data: routeData,
            queryParamMap: routeParams,
            snapshot: {
              queryParamMap: convertToParamMap({ search: 'Luna', page: '0' }),
              paramMap: convertToParamMap({})
            }
          }
        },
        {
          provide: Router,
          useValue: {
            navigate: jasmine.createSpy('navigate').and.resolveTo(true),
            url: '/perros'
          }
        }
      ]
    }).overrideComponent(DogsComponent, { set: { template: '', imports: [] } }).compileComponents();
  });

  it('loads the dog list with the route search and page', () => {
    fixture = TestBed.createComponent(DogsComponent);
    fixture.detectChanges();

    expect(daycareService.getDogs).toHaveBeenCalledWith('Luna', 0, 50);
    expect(fixture.componentInstance.totalElements).toBe(0);
    expect(fixture.componentInstance.isList).toBeTrue();
  });

  it('loads existing tutors for the creation selector', () => {
    routeData.next({ screen: 'create' });
    fixture = TestBed.createComponent(DogsComponent);
    fixture.detectChanges();

    expect(daycareService.getTutors).toHaveBeenCalledWith('', 0, 100);
    expect(fixture.componentInstance.isCreate).toBeTrue();
    expect(fixture.componentInstance.form.tutorId).toBeNull();
  });
});

function emptyDogPage(): DogPage {
  return { items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
}

function emptyTutorPage(): TutorPage {
  return { items: [], page: 0, size: 100, totalElements: 0, totalPages: 0 };
}
