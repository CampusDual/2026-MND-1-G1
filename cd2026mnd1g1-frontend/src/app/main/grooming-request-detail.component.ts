import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  GroomingAppointmentDetail,
  GroomingAppointment,
  GroomingAppointmentStatus,
  GroomingRequestOrigin,
} from '../core/operational.models';

/**
 * Detalle de solo lectura de una cita de peluquería.
 */
@Component({
  selector: 'app-grooming-request-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './grooming-request-detail.component.html',
  styleUrl: './grooming-request-detail.component.scss'
})
export class GroomingRequestDetailComponent implements OnInit {
  appointment: GroomingAppointmentDetail | null = null;
  isLoading: boolean = true;
  hasAccessError: boolean = false;
  errorMessage: string = '';
  private appointmentId: number = 0;
  private agendaGroomerId: number | null = null;
  private agendaDate: string | null = null;
  private agendaReturnTo: string | null = null;
  private sourceAppointmentId: number | null = null;

  constructor(
    private readonly authService: AuthService,
    private readonly daycareOperationsService: DaycareOperationsService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.appointmentId = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(this.appointmentId) || this.appointmentId < 0) {
      this.isLoading = false;
      this.errorMessage = 'No se encontró la cita indicada.';
      return;
    }
    this.agendaGroomerId = this.readId(this.route.snapshot.queryParamMap.get('agendaGroomerId'));
    this.agendaDate = this.route.snapshot.queryParamMap.get('agendaDate');
    this.agendaReturnTo = this.route.snapshot.queryParamMap.get('returnTo');
    this.sourceAppointmentId = this.readId(this.route.snapshot.queryParamMap.get('sourceAppointmentId'));
    this.loadAppointment();
  }

  loadAppointment(): void {
    this.isLoading = true;
    this.hasAccessError = false;
    this.errorMessage = '';
    const request = this.agendaGroomerId === null
      ? this.daycareOperationsService.getReceptionGroomingAppointment(this.appointmentId)
      : this.daycareOperationsService.getReceptionGroomerAgendaAppointment(
        this.agendaGroomerId,
        this.appointmentId
      );
    request.subscribe({
      next: (appointment: GroomingAppointmentDetail) => {
        this.appointment = appointment;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => this.handleError(error)
    });
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
    return origin === 'B2B' ? 'B2B · Guardería cliente' : 'Interna';
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

  locationLabel(location: GroomingAppointment['location']): string {
    if (location === null) {
      return 'Sin asignar';
    }
    return location === 'DESPLAZADO' ? 'Servicio desplazado' : 'En las instalaciones';
  }

  backToList(): void {
    if (this.hasAgendaContext && this.agendaGroomerId !== null && this.agendaDate !== null) {
      this.router.navigate(['/agenda-peluqueros'], {
        queryParams: {
          groomerId: this.agendaGroomerId,
          date: this.agendaDate,
          returnTo: this.agendaReturnTo,
          sourceAppointmentId: this.sourceAppointmentId
        }
      });
      return;
    }
    this.router.navigate(['/citas-peluqueria']);
  }

  get hasAgendaContext(): boolean {
    return this.agendaGroomerId !== null && this.agendaDate !== null;
  }

  openAgenda(): void {
    if (this.appointment === null || this.appointment.status !== 'PENDIENTE_ASIGNACION') {
      return;
    }
    this.router.navigate(['/agenda-peluqueros'], {
      queryParams: {
        date: this.appointment.date,
        returnTo: 'appointment',
        sourceAppointmentId: this.appointment.id
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private handleError(error: HttpErrorResponse): void {
    this.isLoading = false;
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
      ? 'No se encontró la cita indicada.'
      : 'No se pudo cargar el detalle. Comprueba la conexión e inténtalo de nuevo.';
  }

  private readId(value: string | null): number | null {
    if (value === null || !/^\d+$/.test(value)) {
      return null;
    }
    const parsedId: number = Number(value);
    return Number.isSafeInteger(parsedId) && parsedId >= 0 ? parsedId : null;
  }
}
