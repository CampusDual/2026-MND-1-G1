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
    this.loadAppointment();
  }

  loadAppointment(): void {
    this.isLoading = true;
    this.hasAccessError = false;
    this.errorMessage = '';
    this.daycareOperationsService.getReceptionGroomingAppointment(this.appointmentId).subscribe({
      next: (appointment: GroomingAppointmentDetail) => {
        this.appointment = appointment;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => this.handleError(error)
    });
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
      weekday: 'long',
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

  backToList(): void {
    this.router.navigate(['/citas-peluqueria']);
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
}
