import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  GroomerAgenda,
  GroomerOption,
  GroomingAppointment,
  GroomingAppointmentStatus,
  GroomingRequestOrigin
} from '../core/operational.models';

/**
 * Consulta de solo lectura de citas asignadas a peluqueros del centro.
 */
@Component({
  selector: 'app-groomer-agenda',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './groomer-agenda.component.html',
  styleUrl: './groomer-agenda.component.scss'
})
export class GroomerAgendaComponent implements OnInit {
  groomers: GroomerOption[] = [];
  selectedGroomerId: number | null = null;
  date: string = '';
  agenda: GroomerAgenda | null = null;
  isLoadingGroomers: boolean = true;
  isLoadingAgenda: boolean = false;
  hasAccessError: boolean = false;
  errorMessage: string = '';
  private returnTo: 'create' | 'appointment' | 'list' = 'list';
  private sourceAppointmentId: number | null = null;

  constructor(
    private readonly authService: AuthService,
    private readonly daycareOperationsService: DaycareOperationsService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.date = this.route.snapshot.queryParamMap.get('date') || this.localDate();
    this.returnTo = this.readReturnTo(this.route.snapshot.queryParamMap.get('returnTo'));
    this.sourceAppointmentId = this.readId(this.route.snapshot.queryParamMap.get('sourceAppointmentId'));
    const requestedGroomerId: number | null = this.readId(
      this.route.snapshot.queryParamMap.get('groomerId')
    );
    this.loadGroomers(requestedGroomerId);
  }

  loadAgenda(): void {
    this.errorMessage = '';
    this.agenda = null;
    if (this.selectedGroomerId === null || !this.isValidDate(this.date)) {
      this.errorMessage = 'Selecciona un peluquero y una fecha válida.';
      return;
    }
    this.isLoadingAgenda = true;
    this.daycareOperationsService.getReceptionGroomerAgenda(this.selectedGroomerId, this.date).subscribe({
      next: (agenda: GroomerAgenda) => {
        this.agenda = agenda;
        this.isLoadingAgenda = false;
      },
      error: (error: HttpErrorResponse) => this.handleError(error, 'No se pudo cargar la agenda.')
    });
  }

  retryGroomers(): void {
    this.loadGroomers(this.selectedGroomerId);
  }

  openAppointment(appointment: GroomingAppointment): void {
    if (this.selectedGroomerId === null) {
      return;
    }
    this.router.navigate(['/citas-peluqueria', appointment.id], {
      queryParams: {
        agendaGroomerId: this.selectedGroomerId,
        agendaDate: this.date,
        returnTo: this.returnTo,
        sourceAppointmentId: this.sourceAppointmentId
      }
    });
  }

  returnToSource(): void {
    if (this.returnTo === 'appointment' && this.sourceAppointmentId !== null) {
      this.router.navigate(['/citas-peluqueria', this.sourceAppointmentId]);
      return;
    }
    if (this.returnTo === 'create') {
      this.router.navigate(['/citas-peluqueria/nueva']);
      return;
    }
    this.router.navigate(['/citas-peluqueria']);
  }

  statusLabel(status: GroomingAppointmentStatus): string {
    const labels: Record<GroomingAppointmentStatus, string> = {
      PENDIENTE_ASIGNACION: 'Pendiente de asignación',
      SOLICITADA: 'Solicitada',
      CONFIRMADA: 'Confirmada',
      COMPLETADA: 'Completada',
      CANCELADA: 'Cancelada'
    };
    return labels[status];
  }

  originLabel(origin: GroomingRequestOrigin): string {
    return origin === 'B2B' ? 'B2B · Guardería cliente' : 'Guardería interna';
  }

  locationLabel(location: GroomingAppointment['location']): string {
    if (location === null) {
      return 'Sin indicar';
    }
    return location === 'DESPLAZADO' ? 'Servicio desplazado' : 'En las instalaciones';
  }

  formatDate(date: string, time: string): string {
    const formattedDate: string = new Intl.DateTimeFormat('es-ES', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC'
    }).format(new Date(`${date}T12:00:00Z`));
    return `${formattedDate} · ${time.slice(0, 5)}`;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private loadGroomers(requestedGroomerId: number | null): void {
    this.isLoadingGroomers = true;
    this.errorMessage = '';
    this.daycareOperationsService.getReceptionGroomers().subscribe({
      next: (groomers: GroomerOption[]) => {
        this.groomers = groomers;
        this.isLoadingGroomers = false;
        this.selectedGroomerId = groomers.some((groomer) => groomer.id === requestedGroomerId)
          ? requestedGroomerId
          : null;
        if (this.selectedGroomerId !== null) {
          this.loadAgenda();
        }
      },
      error: (error: HttpErrorResponse) => this.handleError(error, 'No se pudieron cargar los peluqueros.')
    });
  }

  private handleError(error: HttpErrorResponse, message: string): void {
    this.isLoadingGroomers = false;
    this.isLoadingAgenda = false;
    if (error.status === 401) {
      const returnUrl: string = this.router.url;
      this.authService.logout();
      this.router.navigate(['/login'], { queryParams: { returnUrl } });
      return;
    }
    if (error.status === 403) {
      this.hasAccessError = true;
      return;
    }
    this.errorMessage = error.status === 404
      ? 'El peluquero o la cita no están disponibles en este centro.'
      : message;
  }

  private readReturnTo(value: string | null): 'create' | 'appointment' | 'list' {
    if (value === 'create' || value === 'appointment') {
      return value;
    }
    return 'list';
  }

  private readId(value: string | null): number | null {
    if (value === null || !/^\d+$/.test(value)) {
      return null;
    }
    const parsedId: number = Number(value);
    return Number.isSafeInteger(parsedId) && parsedId >= 0 ? parsedId : null;
  }

  private isValidDate(value: string): boolean {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return false;
    }
    const parsedDate: Date = new Date(`${value}T00:00:00Z`);
    return !Number.isNaN(parsedDate.getTime()) && parsedDate.toISOString().slice(0, 10) === value;
  }

  private localDate(): string {
    const now: Date = new Date();
    const offset: number = now.getTimezoneOffset() * 60_000;
    return new Date(now.getTime() - offset).toISOString().slice(0, 10);
  }
}
