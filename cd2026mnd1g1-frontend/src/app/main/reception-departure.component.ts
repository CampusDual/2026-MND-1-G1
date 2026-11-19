import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  ReceptionDepartureData,
  ReceptionDepartureRequest,
  ReceptionReservationDetail
} from '../core/operational.models';

/**
 * Registra la salida del perro y sus observaciones opcionales en recepción.
 */
@Component({
  selector: 'app-reception-departure',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reception-departure.component.html',
  styleUrl: './reception-departure.component.scss'
})
export class ReceptionDepartureComponent implements OnInit {
  departureData: ReceptionDepartureData | null = null;
  existingDeparture: ReceptionReservationDetail | null = null;
  selectedDate: string = '';
  collectorType: 'TUTOR' | 'PERSONA_AUTORIZADA' = 'TUTOR';
  authorizedPersonId: number | null = null;
  observations: string = '';
  isLoading: boolean = true;
  isSaving: boolean = false;
  errorMessage: string = '';
  noticeMessage: string = '';
  readonly currentDate: string = this.getTodayDateInCenterZone();

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
    this.loadDepartureData(reservationId, true);
  }

  get selectedCollectorName(): string {
    if (!this.departureData) {
      return '';
    }
    if (this.collectorType === 'TUTOR') {
      return this.departureData.tutorName ?? '';
    }
    return this.departureData.authorizedPersons
      .find((person) => person.id === this.authorizedPersonId)?.name ?? '';
  }

  get isOldStay(): boolean {
    return this.departureData !== null && this.departureData.date < this.currentDate;
  }

  canRegisterDeparture(data: ReceptionDepartureData): boolean {
    const hasCollector: boolean = this.collectorType === 'TUTOR'
      ? data.tutorId !== null && data.tutorName !== null
      : data.authorizedPersons.some((person) => person.id === this.authorizedPersonId);
    return data.reservationStatus === 'EN_CURSO'
      && data.presenceStatus === 'EN_EL_CENTRO'
      && data.entryAt !== null
      && hasCollector;
  }

  statusLabel(status: ReceptionDepartureData['reservationStatus']): string {
    const labels: Record<ReceptionDepartureData['reservationStatus'], string> = {
      PENDIENTE: 'Pendiente',
      CONFIRMADA: 'Confirmada',
      EN_CURSO: 'En curso',
      COMPLETADA: 'Completada',
      CANCELADA: 'Cancelada'
    };
    return labels[status];
  }

  modalityLabel(modality: ReceptionDepartureData['modality']): string {
    const labels: Record<ReceptionDepartureData['modality'], string> = {
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

  formatDateTime(dateTime: string): string {
    const [date, time] = dateTime.split('T');
    return `${this.formatDate(date)}, ${time?.slice(0, 5) ?? ''}`;
  }

  cancel(): void {
    if (this.departureData) {
      this.router.navigate(['/tablero-diario/reservas', this.departureData.reservationId], {
        queryParams: this.getDateQueryParams()
      });
      return;
    }
    this.router.navigate(['/tablero-diario'], { queryParams: this.getDateQueryParams() });
  }

  submit(): void {
    const data: ReceptionDepartureData | null = this.departureData;
    if (!data || !this.canRegisterDeparture(data) || this.isSaving) {
      return;
    }
    if (this.observations.length > 2000) {
      this.errorMessage = 'Las observaciones admiten hasta 2000 caracteres.';
      return;
    }
    const request: ReceptionDepartureRequest = {
      collectorType: this.collectorType,
      authorizedPersonId: this.collectorType === 'PERSONA_AUTORIZADA' ? this.authorizedPersonId : null,
      observations: this.observations.trim() || null
    };
    if (!window.confirm(this.confirmationText(data))) {
      return;
    }
    this.isSaving = true;
    this.errorMessage = '';
    this.noticeMessage = '';
    this.daycareOperationsService.registerReceptionDeparture(data.reservationId, request).subscribe({
      next: () => this.router.navigate(['/tablero-diario/reservas', data.reservationId], {
        queryParams: this.getDateQueryParams()
      }),
      error: (error: HttpErrorResponse) => this.handleSubmitError(error, data.reservationId)
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private loadDepartureData(reservationId: number, initial: boolean): void {
    if (initial) {
      this.isLoading = true;
    }
    this.daycareOperationsService.getReceptionDepartureData(reservationId).subscribe({
      next: (data: ReceptionDepartureData) => {
        this.departureData = data;
        this.isLoading = false;
        if (data.presenceStatus === 'SALIDA_REGISTRADA') {
          this.loadExistingDepartureDetails(reservationId);
        }
        if (this.collectorType === 'PERSONA_AUTORIZADA'
          && !data.authorizedPersons.some((person) => person.id === this.authorizedPersonId)) {
          this.authorizedPersonId = null;
        }
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        if (error.status === 401) {
          this.requireLogin();
        } else if (error.status === 403) {
          this.errorMessage = 'Tu cuenta no tiene permiso para registrar salidas.';
        } else if (error.status === 404) {
          this.errorMessage = 'No se encontró la reserva solicitada en este centro.';
        } else {
          this.errorMessage = 'No se pudieron cargar los datos de salida. Comprueba la conexión e inténtalo de nuevo.';
        }
      }
    });
  }

  private loadExistingDepartureDetails(reservationId: number): void {
    this.daycareOperationsService.getReceptionReservation(reservationId).subscribe({
      next: (reservation: ReceptionReservationDetail) => {
        this.existingDeparture = reservation;
      },
      error: () => {
        this.noticeMessage = 'La salida ya está registrada. No se pudo cargar su detalle actualizado; vuelve al detalle de la reserva.';
      }
    });
  }

  private handleSubmitError(error: HttpErrorResponse, reservationId: number): void {
    this.isSaving = false;
    if (error.status === 401) {
      this.requireLogin();
      return;
    }
    if (error.status === 0) {
      this.checkDepartureAfterNetworkError(reservationId);
      return;
    }
    const serverMessage: unknown = error.error?.message;
    this.errorMessage = typeof serverMessage === 'string'
      ? serverMessage
      : 'No se pudo registrar la salida. Los datos introducidos se han conservado.';
    if (error.status === 409) {
      this.loadDepartureData(reservationId, false);
      this.noticeMessage = 'El estado de la reserva pudo cambiar. Se ha actualizado la información antes de reintentar.';
    }
  }

  private checkDepartureAfterNetworkError(reservationId: number): void {
    this.daycareOperationsService.getReceptionReservation(reservationId).subscribe({
      next: (reservation: ReceptionReservationDetail) => {
        this.isSaving = false;
        if (reservation.presence.status === 'SALIDA_REGISTRADA') {
          this.router.navigate(['/tablero-diario/reservas', reservationId], {
            queryParams: this.getDateQueryParams()
          });
          return;
        }
        this.errorMessage = 'No se pudo confirmar si la salida se guardó. Comprueba el estado antes de reintentar.';
        this.loadDepartureData(reservationId, false);
      },
      error: () => {
        this.isSaving = false;
        this.errorMessage = 'No se pudo confirmar si la salida se guardó. Consulta el detalle antes de reintentar.';
      }
    });
  }

  private confirmationText(data: ReceptionDepartureData): string {
    return `Confirmar la salida de ${data.dogName} y entregarlo a ${this.selectedCollectorName}?\n\n`
      + `Observaciones: ${this.observations.trim() || 'Ninguna'}`;
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
