import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import { ReceptionReservationDetail } from '../core/operational.models';

/**
 * Formulario de llegada de una reserva, sin datos de hora o responsable editables.
 */
@Component({
  selector: 'app-reception-arrival',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reception-arrival.component.html',
  styleUrl: './reception-arrival.component.scss'
})
export class ReceptionArrivalComponent implements OnInit {
  reservation: ReceptionReservationDetail | null = null;
  selectedDate: string = '';
  observations: string = '';
  isLoading: boolean = true;
  isSaving: boolean = false;
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
    if (!Number.isSafeInteger(reservationId) || reservationId < 0) {
      this.isLoading = false;
      this.errorMessage = 'No se encontró la reserva solicitada.';
      return;
    }
    this.loadReservation(reservationId);
  }

  canRegisterArrival(item: ReceptionReservationDetail): boolean {
    return item.date === this.getTodayDateInCenterZone()
      && item.status === 'CONFIRMADA'
      && item.presence.status === 'SIN_ENTRADA_REGISTRADA';
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

  formatDate(date: string): string {
    return new Intl.DateTimeFormat('es-ES', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC'
    }).format(new Date(`${date}T12:00:00Z`));
  }

  cancel(): void {
    const queryParams: Record<string, string> = this.getDateQueryParams();
    if (this.reservation) {
      this.router.navigate(['/tablero-diario/reservas', this.reservation.id], { queryParams });
      return;
    }
    this.router.navigate(['/tablero-diario'], { queryParams });
  }

  submit(): void {
    if (!this.reservation || !this.canRegisterArrival(this.reservation) || this.isSaving) {
      return;
    }
    if (this.observations.length > 2000) {
      this.errorMessage = 'Las observaciones no pueden superar 2000 caracteres.';
      return;
    }
    this.isSaving = true;
    this.errorMessage = '';
    this.daycareOperationsService.registerReceptionArrival(this.reservation.id, {
      observations: this.observations.trim() || null
    }).subscribe({
      next: () => this.router.navigate(['/tablero-diario/reservas', this.reservation?.id], {
        queryParams: this.getDateQueryParams()
      }),
      error: (error: HttpErrorResponse) => this.handleSubmitError(error)
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private loadReservation(reservationId: number): void {
    this.daycareOperationsService.getReceptionReservation(reservationId).subscribe({
      next: (reservation: ReceptionReservationDetail) => {
        this.reservation = reservation;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        if (error.status === 401) {
          this.requireLogin();
        } else if (error.status === 404) {
          this.errorMessage = 'No se encontró la reserva solicitada en este centro.';
        } else if (error.status === 403) {
          this.errorMessage = 'Tu cuenta no tiene permiso para registrar llegadas.';
        } else {
          this.errorMessage = 'No se pudo cargar la reserva. Comprueba la conexión e inténtalo de nuevo.';
        }
      }
    });
  }

  private handleSubmitError(error: HttpErrorResponse): void {
    this.isSaving = false;
    if (error.status === 401) {
      this.requireLogin();
      return;
    }
    if (error.status === 0) {
      this.checkArrivalAfterNetworkError();
      return;
    }
    const serverMessage: unknown = error.error?.message;
    this.errorMessage = typeof serverMessage === 'string'
      ? serverMessage
      : 'No se pudo registrar la llegada. Los datos introducidos se han conservado.';
    if (error.status === 409 && this.reservation) {
      this.loadReservation(this.reservation.id);
    }
  }

  private checkArrivalAfterNetworkError(): void {
    if (!this.reservation) {
      this.errorMessage = 'No se pudo confirmar el resultado. Consulta de nuevo el detalle de la reserva.';
      return;
    }
    this.daycareOperationsService.getReceptionReservation(this.reservation.id).subscribe({
      next: (reservation: ReceptionReservationDetail) => {
        this.reservation = reservation;
        this.isSaving = false;
        this.errorMessage = reservation.presence.status === 'EN_EL_CENTRO'
          ? 'La llegada ya está registrada. Se ha actualizado el estado de la reserva.'
          : 'No se pudo confirmar si la llegada se guardó. Comprueba el estado antes de volver a intentarlo.';
      },
      error: () => {
        this.isSaving = false;
        this.errorMessage = 'No se pudo confirmar si la llegada se guardó. Actualiza el detalle antes de reintentar.';
      }
    });
  }

  private getDateQueryParams(): Record<string, string> {
    return this.selectedDate ? { date: this.selectedDate } : {};
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

  private requireLogin(): void {
    const returnUrl: string = this.router.url;
    this.authService.logout();
    this.router.navigate(['/login'], { queryParams: { returnUrl } });
  }
}
