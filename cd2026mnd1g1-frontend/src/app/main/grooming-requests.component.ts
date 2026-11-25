import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, Subject, Subscription, catchError, map, of, switchMap, tap } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  GroomingAppointment,
  GroomingAppointmentStatus,
  GroomingRequestOrigin,
  GroomingAppointmentPage,
} from '../core/operational.models';

type AppointmentLoadResult =
  | { kind: 'success'; page: GroomingAppointmentPage }
  | { kind: 'error'; error: HttpErrorResponse };

/**
 * Listado de solo lectura de citas de peluquería para recepción.
 */
@Component({
  selector: 'app-grooming-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './grooming-requests.component.html',
  styleUrl: './grooming-requests.component.scss'
})
export class GroomingRequestsComponent implements OnInit, OnDestroy {
  private readonly pageSize: number = 25;
  private readonly loadAppointmentsSubject: Subject<void> = new Subject();
  private readonly subscriptions: Subscription = new Subscription();

  readonly statuses: GroomingAppointmentStatus[] = ['SOLICITADA', 'CONFIRMADA', 'COMPLETADA', 'CANCELADA'];
  origin: GroomingRequestOrigin | '' = '';
  status: GroomingAppointmentStatus | '' = '';
  fromDate: string = '';
  toDate: string = '';
  appointments: GroomingAppointment[] = [];
  currentPage: number = 0;
  totalPages: number = 0;
  totalElements: number = 0;
  isLoading: boolean = true;
  hasAccessError: boolean = false;
  errorMessage: string = '';

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly daycareOperationsService: DaycareOperationsService
  ) {}

  ngOnInit(): void {
    this.subscriptions.add(this.loadAppointmentsSubject.pipe(
      tap(() => {
        this.isLoading = true;
        this.hasAccessError = false;
        this.errorMessage = '';
        this.appointments = [];
        this.totalElements = 0;
        this.totalPages = 0;
      }),
      switchMap((): Observable<AppointmentLoadResult> =>
        this.daycareOperationsService.getReceptionGroomingAppointments(
          { origin: this.origin, status: this.status, from: this.fromDate, to: this.toDate },
          this.currentPage,
          this.pageSize
        ).pipe(
          map((page: GroomingAppointmentPage): AppointmentLoadResult => ({ kind: 'success', page })),
          catchError((error: HttpErrorResponse) => of<AppointmentLoadResult>({ kind: 'error', error }))
        )
      )
    ).subscribe((result: AppointmentLoadResult) => this.handleLoadResult(result)));
    this.loadAppointments();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  applyFilters(): void {
    if (!this.isValidDateRange()) {
      return;
    }
    this.currentPage = 0;
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.loadAppointmentsSubject.next();
  }

  changePage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.currentPage = page;
      this.loadAppointments();
    }
  }

  statusLabel(status: GroomingAppointmentStatus): string {
    const labels: Record<GroomingAppointmentStatus, string> = {
      SOLICITADA: 'Solicitada',
      CONFIRMADA: 'Confirmada',
      COMPLETADA: 'Completada',
      CANCELADA: 'Cancelada'
    };
    return labels[status];
  }

  originLabel(origin: GroomingRequestOrigin): string {
    return origin === 'B2B' ? 'B2B · Guardería cliente' : 'Interna';
  }

  formatDate(date: string, time: string): string {
    const formattedDate: string = new Intl.DateTimeFormat('es-ES', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC'
    }).format(new Date(`${date}T12:00:00Z`));
    return `${formattedDate} · ${time.slice(0, 5)}`;
  }

  locationLabel(location: GroomingAppointment['location']): string {
    return location === 'DESPLAZADO' ? 'Servicio desplazado' : 'En las instalaciones';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private handleLoadResult(result: AppointmentLoadResult): void {
    this.isLoading = false;
    if (result.kind === 'success') {
      this.appointments = result.page.items;
      this.currentPage = result.page.page;
      this.totalPages = result.page.totalPages;
      this.totalElements = result.page.totalElements;
      return;
    }
    if (result.error.status === 401) {
      const returnUrl: string = this.router.url;
      this.authService.logout();
      this.router.navigate(['/login'], { queryParams: { returnUrl } });
      return;
    }
    if (result.error.status === 403) {
      this.hasAccessError = true;
      return;
    }
    this.errorMessage = result.error.status === 400
      ? 'Los filtros indicados no tienen un formato válido.'
      : 'No se pudieron cargar las citas. Comprueba la conexión e inténtalo de nuevo.';
  }

  private isValidDateRange(): boolean {
    if ((this.fromDate && !this.isValidDate(this.fromDate))
        || (this.toDate && !this.isValidDate(this.toDate))) {
      this.errorMessage = 'La fecha indicada no es válida.';
      this.isLoading = false;
      return false;
    }
    if (this.fromDate && this.toDate && this.fromDate > this.toDate) {
      this.errorMessage = 'La fecha inicial no puede ser posterior a la fecha final.';
      this.isLoading = false;
      return false;
    }
    return true;
  }

  private isValidDate(value: string): boolean {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return false;
    }
    const parsedDate: Date = new Date(`${value}T00:00:00Z`);
    return !Number.isNaN(parsedDate.getTime()) && parsedDate.toISOString().slice(0, 10) === value;
  }
}
