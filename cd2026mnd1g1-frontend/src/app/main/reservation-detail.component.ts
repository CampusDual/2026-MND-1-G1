import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import { ReceptionReservationDetail } from '../core/operational.models';

/**
 * Detalle de solo lectura de una reserva para recepción.
 */
@Component({
  selector: 'app-reservation-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './reservation-detail.component.html',
  styleUrl: './reservation-detail.component.scss'
})
export class ReservationDetailComponent implements OnInit {
  reservation: ReceptionReservationDetail | null = null;
  selectedDate: string = '';
  isLoading: boolean = true;
  hasAccessError: boolean = false;
  errorMessage: string = '';

  constructor(
    private readonly authService: AuthService,
    private readonly daycareOperationsService: DaycareOperationsService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.selectedDate = this.route.snapshot.queryParamMap.get('date') ?? '';
    const reservationId: number = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(reservationId) || reservationId <= 0) {
      this.isLoading = false;
      this.errorMessage = 'No se encontró la reserva solicitada.';
      return;
    }

    this.daycareOperationsService.getReceptionReservation(reservationId).subscribe({
      next: (reservation: ReceptionReservationDetail) => {
        this.reservation = reservation;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => this.handleError(error)
    });
  }

  statusLabel(status: ReceptionReservationDetail['status']): string {
    const labels: Record<ReceptionReservationDetail['status'], string> = {
      PENDIENTE: 'Pendiente',
      CONFIRMADA: 'Confirmada',
      EN_CURSO: 'En curso',
      COMPLETADA: 'Completada',
      CANCELADA: 'Cancelada'
    };
    return labels[status];
  }

  modalityLabel(modality: ReceptionReservationDetail['modality']): string {
    const labels: Record<ReceptionReservationDetail['modality'], string> = {
      DIA_COMPLETO: 'Día completo',
      MEDIA_JORNADA: 'Media jornada (franja histórica desconocida)',
      MANANA: 'Mañana',
      TARDE: 'Tarde'
    };
    return labels[modality];
  }

  presenceLabel(status: ReceptionReservationDetail['presence']['status']): string {
    const labels: Record<ReceptionReservationDetail['presence']['status'], string> = {
      SIN_ENTRADA_REGISTRADA: 'Sin entrada registrada',
      EN_EL_CENTRO: 'En el centro',
      SALIDA_REGISTRADA: 'Salida registrada',
      REGISTRO_INCOHERENTE: 'Registro incoherente: salida sin entrada'
    };
    return labels[status];
  }

  canRegisterArrival(item: ReceptionReservationDetail): boolean {
    return item.date === this.getTodayDateInCenterZone()
      && item.status === 'CONFIRMADA'
      && item.presence.status === 'SIN_ENTRADA_REGISTRADA';
  }

  canRegisterDeparture(item: ReceptionReservationDetail): boolean {
    return item.status === 'EN_CURSO'
      && item.presence.status === 'EN_EL_CENTRO'
      && item.presence.lastEntryAt !== null;
  }

  formatDate(date: string): string {
    return new Intl.DateTimeFormat('es-ES', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC'
    }).format(new Date(`${date}T12:00:00Z`));
  }

  formatDateTime(dateTime: string): string {
    const [date, time] = dateTime.split('T');
    const displayTime: string = time?.slice(0, 5) ?? '';
    return `${this.formatDate(date)}, ${displayTime}`;
  }

  backToList(): void {
    const queryParams: Record<string, string> = this.selectedDate
      ? { date: this.selectedDate }
      : {};
    this.router.navigate(['/tablero-diario'], { queryParams });
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
      ? 'No se encontró la reserva solicitada.'
      : 'No se pudo cargar el detalle. Comprueba la conexión e inténtalo de nuevo.';
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
