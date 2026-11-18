import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  Observable,
  Subject,
  Subscription,
  catchError,
  map,
  of,
  switchMap,
  tap
} from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  Dog,
  DogPage,
  ReceptionAvailability,
  ReceptionGroupOption,
  ReservationBookingModality,
  ReceptionReservationCreateRequest
} from '../core/operational.models';

type AvailabilityResult =
  | { kind: 'success'; availability: ReceptionAvailability }
  | { kind: 'error'; error: HttpErrorResponse }
  | { kind: 'idle' };

/**
 * Permite crear una reserva de un perro en un grupo y franja disponibles.
 */
@Component({
  selector: 'app-reservation-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reservation-create.component.html',
  styleUrl: './reservation-create.component.scss'
})
export class ReservationCreateComponent implements OnInit, OnDestroy {
  private readonly subscriptions: Subscription = new Subscription();
  private readonly availabilityRequests: Subject<{ date: string; groupId: number } | null> = new Subject();
  private dogSearchSubscription: Subscription | null = null;
  private readonly pageSize: number = 50;

  readonly modalities: { value: ReservationBookingModality; label: string }[] = [
    { value: 'DIA_COMPLETO', label: 'Día completo' },
    { value: 'MANANA', label: 'Mañana' },
    { value: 'TARDE', label: 'Tarde' }
  ];
  readonly today: string = this.getToday();

  search: string = '';
  dogs: Dog[] = [];
  selectedDogId: number | null = null;
  selectedDog: Dog | null = null;
  dogPage: number = 0;
  dogTotalPages: number = 0;
  dogTotalElements: number = 0;
  groups: ReceptionGroupOption[] = [];
  selectedGroupId: number | null = null;
  selectedDate: string = '';
  selectedModality: ReservationBookingModality = 'DIA_COMPLETO';
  availability: ReceptionAvailability | null = null;
  isLoadingGroups: boolean = true;
  isLoadingDogs: boolean = true;
  isLoadingAvailability: boolean = false;
  isSubmitting: boolean = false;
  availabilityError: string = '';
  bookingError: string = '';
  pageError: string = '';
  hasAccessError: boolean = false;

  constructor(
    private readonly authService: AuthService,
    private readonly daycareOperationsService: DaycareOperationsService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const requestedDate: string | null = this.route.snapshot.queryParamMap.get('date');
    this.selectedDate = requestedDate !== null && this.isAllowedDate(requestedDate)
      ? requestedDate
      : this.getNextOperationalDate();

    this.subscriptions.add(this.availabilityRequests.pipe(
      tap(() => {
        this.isLoadingAvailability = true;
        this.availability = null;
        this.availabilityError = '';
      }),
      switchMap((request): Observable<AvailabilityResult> => {
        if (request === null) {
          return of<AvailabilityResult>({ kind: 'idle' });
        }
        return this.daycareOperationsService.getReceptionAvailability(request.date, request.groupId).pipe(
          map((result: ReceptionAvailability): AvailabilityResult => ({ kind: 'success', availability: result })),
          catchError((error: HttpErrorResponse) => of<AvailabilityResult>({ kind: 'error', error }))
        );
      }
      )
    ).subscribe((result: AvailabilityResult) => this.handleAvailabilityResult(result)));

    this.loadGroups();
    this.loadDogs();
  }

  ngOnDestroy(): void {
    this.dogSearchSubscription?.unsubscribe();
    this.subscriptions.unsubscribe();
  }

  searchDogs(): void {
    this.dogPage = 0;
    this.loadDogs();
  }

  changeDogPage(page: number): void {
    if (page >= 0 && page < this.dogTotalPages && page !== this.dogPage) {
      this.dogPage = page;
      this.loadDogs();
    }
  }

  selectDog(dogId: number | null): void {
    this.selectedDogId = dogId;
    this.selectedDog = this.dogs.find((dog) => dog.id === dogId) ?? null;
    this.bookingError = '';
  }

  onBookingCriteriaChanged(): void {
    this.bookingError = '';
    this.requestAvailability();
  }

  availableForSelection(): boolean {
    if (this.availability === null) {
      return false;
    }
    if (this.selectedModality === 'DIA_COMPLETO') {
      return this.availability.morningRemaining > 0 && this.availability.afternoonRemaining > 0;
    }
    return this.selectedModality === 'MANANA'
      ? this.availability.morningRemaining > 0
      : this.availability.afternoonRemaining > 0;
  }

  canSubmit(): boolean {
    return this.selectedDogId !== null
      && this.selectedDog !== null
      && this.selectedDog.tutorId !== null
      && this.selectedGroupId !== null
      && this.isAllowedDate(this.selectedDate)
      && this.availability !== null
      && this.availability.date === this.selectedDate
      && this.availability.groupId === this.selectedGroupId
      && !this.isLoadingAvailability
      && !this.isSubmitting
      && !this.availabilityError
      && this.availableForSelection();
  }

  saveReservation(): void {
    const dogId: number | null = this.selectedDogId;
    const groupId: number | null = this.selectedGroupId;
    if (!this.canSubmit() || dogId === null || groupId === null) {
      return;
    }
    const request: ReceptionReservationCreateRequest = {
      dogId,
      groupId,
      date: this.selectedDate,
      modality: this.selectedModality
    };
    this.isSubmitting = true;
    this.bookingError = '';
    this.subscriptions.add(this.daycareOperationsService.createReceptionReservation(request).subscribe({
      next: (created) => {
        this.isSubmitting = false;
        this.router.navigate(['/tablero-diario/reservas', created.id], {
          queryParams: { date: created.date }
        });
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting = false;
        if (error.status === 401 || error.status === 403) {
          this.handleAccessError(error);
          return;
        }
        this.bookingError = this.getServerMessage(error)
          || 'No se pudo guardar la reserva. Revisa los datos e inténtalo de nuevo.';
        if (error.status === 409) {
          this.requestAvailability();
        }
      }
    }));
  }

  cancel(): void {
    this.router.navigate(['/tablero-diario'], { queryParams: { date: this.selectedDate } });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  displayDate(date: string): string {
    return new Intl.DateTimeFormat('es-ES', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC'
    }).format(new Date(`${date}T12:00:00Z`));
  }

  private loadGroups(): void {
    this.isLoadingGroups = true;
    this.subscriptions.add(this.daycareOperationsService.getReceptionGroups().subscribe({
      next: (groups: ReceptionGroupOption[]) => {
        this.groups = groups;
        this.isLoadingGroups = false;
      },
      error: (error: HttpErrorResponse) => {
        this.isLoadingGroups = false;
        this.handlePageError(error);
      }
    }));
  }

  private loadDogs(): void {
    this.isLoadingDogs = true;
    this.dogSearchSubscription?.unsubscribe();
    this.dogSearchSubscription = this.daycareOperationsService
      .getReservationDogs(this.search, this.dogPage, this.pageSize)
      .subscribe({
        next: (page: DogPage) => {
          this.dogs = page.items;
          this.dogPage = page.page;
          this.dogTotalPages = page.totalPages;
          this.dogTotalElements = page.totalElements;
          this.isLoadingDogs = false;
        },
        error: (error: HttpErrorResponse) => {
          this.isLoadingDogs = false;
          this.handlePageError(error);
        }
      });
  }

  private requestAvailability(): void {
    this.availabilityRequests.next(null);
    this.availability = null;
    this.availabilityError = '';
    if (!this.isAllowedDate(this.selectedDate)) {
      this.availabilityError = 'Selecciona hoy o una fecha futura de lunes a sábado.';
      return;
    }
    if (this.selectedGroupId === null) {
      return;
    }
    this.availabilityRequests.next({
      date: this.selectedDate,
      groupId: this.selectedGroupId
    });
  }

  private handleAvailabilityResult(result: AvailabilityResult): void {
    this.isLoadingAvailability = false;
    if (result.kind === 'idle') {
      return;
    }
    if (result.kind === 'success') {
      this.availability = result.availability;
      return;
    }
    if (result.error.status === 401 || result.error.status === 403) {
      this.handleAccessError(result.error);
      return;
    }
    this.availabilityError = this.getServerMessage(result.error)
      || 'No se pudo consultar la disponibilidad. No se mostrarán plazas estimadas.';
  }

  private handlePageError(error: HttpErrorResponse): void {
    if (error.status === 401 || error.status === 403) {
      this.handleAccessError(error);
      return;
    }
    this.pageError = this.getServerMessage(error)
      || 'No se pudieron cargar los datos necesarios para crear la reserva.';
  }

  private handleAccessError(error: HttpErrorResponse): void {
    if (error.status === 401) {
      const returnUrl: string = this.router.url;
      this.authService.logout();
      this.router.navigate(['/login'], { queryParams: { returnUrl } });
      return;
    }
    this.hasAccessError = true;
  }

  private getServerMessage(error: HttpErrorResponse): string {
    const body: unknown = error.error;
    if (typeof body === 'object' && body !== null && 'message' in body) {
      const message: unknown = (body as { message?: unknown }).message;
      return typeof message === 'string' ? message : '';
    }
    return '';
  }

  private isAllowedDate(value: string): boolean {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return false;
    }
    const parsedDate: Date = new Date(`${value}T00:00:00Z`);
    return !Number.isNaN(parsedDate.getTime())
      && parsedDate.toISOString().slice(0, 10) === value
      && value >= this.today
      && parsedDate.getUTCDay() !== 0;
  }

  private getToday(): string {
    const parts: Intl.DateTimeFormatPart[] = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Europe/Madrid',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).formatToParts(new Date());
    const values: Map<string, string> = new Map(parts.map((part) => [part.type, part.value]));
    return `${values.get('year')}-${values.get('month')}-${values.get('day')}`;
  }

  private getNextOperationalDate(): string {
    const todayDate: Date = new Date(`${this.today}T00:00:00Z`);
    if (todayDate.getUTCDay() === 0) {
      todayDate.setUTCDate(todayDate.getUTCDate() + 1);
    }
    return todayDate.toISOString().slice(0, 10);
  }
}
