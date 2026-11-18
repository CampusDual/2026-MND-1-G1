import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, Subject, Subscription, catchError, map, of, switchMap, tap } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  ReceptionPresenceStatus,
  ReceptionReservation,
  ReceptionReservationPage
} from '../core/operational.models';

type ReservationLoadResult =
  | { kind: 'success'; page: ReceptionReservationPage }
  | { kind: 'error'; error: HttpErrorResponse };

/**
 * Consulta de solo lectura de reservas para recepción.
 */
@Component({
  selector: 'app-it-works',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './it-works.component.html',
  styleUrls: ['./it-works.component.scss']
})
export class ItWorksComponent implements OnInit, OnDestroy {
  private readonly pageSize: number = 50;
  private readonly loadRequests: Subject<{ date: string; page: number }> = new Subject();
  private readonly subscriptions: Subscription = new Subscription();

  selectedDate: string = '';
  reservations: ReceptionReservation[] = [];
  currentPage: number = 0;
  totalPages: number = 0;
  totalElements: number = 0;
  isLoading: boolean = true;
  hasAccessError: boolean = false;
  errorMessage: string = '';

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly daycareOperationsService: DaycareOperationsService
  ) {}

  ngOnInit(): void {
    this.subscriptions.add(this.loadRequests.pipe(
      tap(() => {
        this.isLoading = true;
        this.hasAccessError = false;
        this.errorMessage = '';
        this.reservations = [];
        this.totalElements = 0;
        this.totalPages = 0;
      }),
      switchMap(({ date, page }): Observable<ReservationLoadResult> =>
        this.daycareOperationsService.getReceptionReservations(date, page, this.pageSize).pipe(
          map((result: ReceptionReservationPage): ReservationLoadResult => ({ kind: 'success', page: result })),
          catchError((error: HttpErrorResponse) =>
            of<ReservationLoadResult>({ kind: 'error', error })
          )
        )
      )
    ).subscribe((result: ReservationLoadResult) => this.handleLoadResult(result)));

    this.subscriptions.add(this.route.queryParamMap.subscribe((parameters) => {
      this.selectedDate = parameters.get('date') ?? this.getTodayDateInCenterZone();
      this.requestReservations(0);
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  onDateChanged(): void {
    if (!this.isValidDate(this.selectedDate)) {
      this.reservations = [];
      this.errorMessage = 'La fecha indicada no es válida.';
      this.isLoading = false;
      return;
    }

    const currentDate: string | null = this.route.snapshot.queryParamMap.get('date');
    if (currentDate === this.selectedDate) {
      this.requestReservations(0);
      return;
    }
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { date: this.selectedDate },
      replaceUrl: true
    });
  }

  loadReservations(): void {
    this.requestReservations(this.currentPage);
  }

  changePage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.requestReservations(page);
    }
  }

  statusLabel(status: ReceptionReservation['status']): string {
    const labels: Record<ReceptionReservation['status'], string> = {
      PENDIENTE: 'Pendiente',
      CONFIRMADA: 'Confirmada',
      EN_CURSO: 'En curso',
      COMPLETADA: 'Completada',
      CANCELADA: 'Cancelada'
    };
    return labels[status];
  }

  modalityLabel(modality: ReceptionReservation['modality']): string {
    const labels: Record<ReceptionReservation['modality'], string> = {
      DIA_COMPLETO: 'Día completo',
      MEDIA_JORNADA: 'Media jornada (franja histórica desconocida)',
      MANANA: 'Mañana',
      TARDE: 'Tarde'
    };
    return labels[modality];
  }

  presenceLabel(status: ReceptionPresenceStatus): string {
    const labels: Record<ReceptionPresenceStatus, string> = {
      SIN_ENTRADA_REGISTRADA: 'Sin entrada registrada',
      EN_EL_CENTRO: 'En el centro',
      SALIDA_REGISTRADA: 'Salida registrada',
      REGISTRO_INCOHERENTE: 'Registro incoherente: salida sin entrada'
    };
    return labels[status];
  }

  displayDate(date: string): string {
    if (!this.isValidDate(date)) {
      return date;
    }
    return new Intl.DateTimeFormat('es-ES', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC'
    }).format(new Date(`${date}T12:00:00Z`));
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private requestReservations(page: number): void {
    this.currentPage = page;
    this.loadRequests.next({ date: this.selectedDate, page });
  }

  private handleLoadResult(result: ReservationLoadResult): void {
    this.isLoading = false;
    if (result.kind === 'success') {
      this.reservations = result.page.items;
      this.currentPage = result.page.page;
      this.totalPages = result.page.totalPages;
      this.totalElements = result.page.totalElements;
      return;
    }

    if (result.error.status === 401) {
      this.requireLogin();
      return;
    }
    if (result.error.status === 403) {
      this.hasAccessError = true;
      return;
    }
    this.errorMessage = result.error.status === 400
      ? 'La fecha o la página solicitada no tienen un formato válido.'
      : 'No se pudieron cargar las reservas. Comprueba la conexión e inténtalo de nuevo.';
  }

  private requireLogin(): void {
    const returnUrl: string = this.router.url;
    this.authService.logout();
    this.router.navigate(['/login'], { queryParams: { returnUrl } });
  }

  private isValidDate(value: string): boolean {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return false;
    }
    const parsedDate: Date = new Date(`${value}T00:00:00Z`);
    return !Number.isNaN(parsedDate.getTime()) && parsedDate.toISOString().slice(0, 10) === value;
  }

  private getTodayDateInCenterZone(): string {
    const parts: Intl.DateTimeFormatPart[] = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Europe/Madrid',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).formatToParts(new Date());
    const values: Map<string, string> = new Map(parts.map((part) => [part.type, part.value]));
    return `${values.get('year')}-${values.get('month')}-${values.get('day')}`;
  }
}
