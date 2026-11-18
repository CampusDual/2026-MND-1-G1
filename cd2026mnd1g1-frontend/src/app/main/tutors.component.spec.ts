import { convertToParamMap, ActivatedRoute, Router } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import { TutorPage } from '../core/operational.models';
import { TutorsComponent } from './tutors.component';

describe('TutorsComponent', () => {
  let fixture: ComponentFixture<TutorsComponent>;
  let daycareService: jasmine.SpyObj<DaycareOperationsService>;

  beforeEach(async () => {
    daycareService = jasmine.createSpyObj<DaycareOperationsService>(
      'DaycareOperationsService',
      ['getTutors', 'getTutor', 'createTutor', 'updateTutor']
    );
    daycareService.getTutors.and.returnValue(of(emptyTutorPage()));

    await TestBed.configureTestingModule({
      imports: [TutorsComponent],
      providers: [
        { provide: AuthService, useValue: { logout: jasmine.createSpy('logout') } },
        { provide: DaycareOperationsService, useValue: daycareService },
        {
          provide: ActivatedRoute,
          useValue: {
            data: new BehaviorSubject({ screen: 'list' }),
            queryParamMap: new BehaviorSubject(convertToParamMap({ search: 'Ana', page: '0' })),
            snapshot: {
              queryParamMap: convertToParamMap({ search: 'Ana', page: '0' }),
              paramMap: convertToParamMap({})
            }
          }
        },
        {
          provide: Router,
          useValue: {
            navigate: jasmine.createSpy('navigate').and.resolveTo(true),
            url: '/tutores'
          }
        }
      ]
    }).overrideComponent(TutorsComponent, { set: { template: '', imports: [] } }).compileComponents();
  });

  it('loads the list using the selected search and center scope is left to the backend', () => {
    fixture = TestBed.createComponent(TutorsComponent);
    fixture.detectChanges();

    expect(daycareService.getTutors).toHaveBeenCalledWith('Ana', 0, 50);
    expect(fixture.componentInstance.totalElements).toBe(0);
    expect(fixture.componentInstance.isList).toBeTrue();
  });
});

function emptyTutorPage(): TutorPage {
  return { items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
}
