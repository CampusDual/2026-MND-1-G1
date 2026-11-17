import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  DailyRecordType,
  DailyReservation,
  IncidentSeverity,
  ReservationGroup
} from '../core/operational.models';
/**
 * Tablero diario de reservas y operaciones rápidas de guardería.
 */
@Component({
  selector: 'app-it-works',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './it-works.component.html',
  styleUrls: ['./it-works.component.scss', './it-works-actions.component.scss']
})
export class ItWorksComponent implements OnInit {
  readonly activityTypes: DailyRecordType[] = ['COMIDA', 'PASEO', 'JUEGO', 'DESCANSO', 'OTRO'];
  readonly incidentSeverities: IncidentSeverity[] = ['LEVE', 'MODERADA', 'GRAVE'];
  readonly todayLabel: string = new Intl.DateTimeFormat('es-ES', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date());
  groups: ReservationGroup[] = [];
  isLoading: boolean = true;
  isSaving: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';
  activeForm: { reservationId: number; kind: 'actividad' | 'incidencia' } | null = null;
  selectedActivity: DailyRecordType = 'COMIDA';
  activityNotes: string = '';
  selectedSeverity: IncidentSeverity = 'LEVE';
  incidentDescription: string = '';

  get totalReservations(): number {
    return this.groups.reduce(
      (total: number, group: ReservationGroup) => total + group.reservations.length,
      0
    );
  }

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly daycareOperationsService: DaycareOperationsService
  ) {}

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.daycareOperationsService.getDailyReservations().subscribe({
      next: (reservations: DailyReservation[]) => {
        this.groups = this.groupReservations(
          reservations.filter((reservation: DailyReservation) => reservation.status !== 'CANCELADA')
        );
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.getErrorMessage(error, 'No se pudieron cargar las reservas del día.');
        this.isLoading = false;
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  groupReservations(reservations: DailyReservation[]): ReservationGroup[] {
    const groups = new Map<number | null, ReservationGroup>();
    for (const reservation of reservations) {
      const groupId: number | null = reservation.groupOccupancy?.groupId ?? null;
      let group: ReservationGroup | undefined = groups.get(groupId);
      if (!group) {
        group = {
          id: groupId,
          label: groupId === null ? 'Sin grupo' : `Grupo ${groupId}`,
          occupancy: reservation.groupOccupancy,
          reservations: []
        };
        groups.set(groupId, group);
      }
      group.reservations.push(reservation);
    }
    return [...groups.values()].sort((first: ReservationGroup, second: ReservationGroup) => {
      if (first.id === null) {
        return 1;
      }
      if (second.id === null) {
        return -1;
      }
      return first.id - second.id;
    });
  }

  recordCheck(reservation: DailyReservation, type: 'ENTRADA' | 'SALIDA'): void {
    this.clearMessages();
    this.isSaving = true;
    this.daycareOperationsService.createCheckRecord(reservation.id, { type }).subscribe({
      next: () => {
        this.successMessage = type === 'ENTRADA'
          ? `Entrada registrada para ${reservation.dog.name}.`
          : `Salida registrada para ${reservation.dog.name}.`;
        this.loadReservations();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.getErrorMessage(error, 'No se pudo registrar la entrada o salida.');
        this.isSaving = false;
      },
      complete: () => {
        this.isSaving = false;
      }
    });
  }

  openForm(reservationId: number, kind: 'actividad' | 'incidencia'): void {
    this.clearMessages();
    this.activeForm = { reservationId, kind };
    this.selectedActivity = 'COMIDA';
    this.activityNotes = '';
    this.selectedSeverity = 'LEVE';
    this.incidentDescription = '';
  }

  closeForm(): void {
    this.activeForm = null;
  }

  submitQuickAction(reservation: DailyReservation): void {
    if (!this.activeForm || this.activeForm.reservationId !== reservation.id || this.isSaving) {
      return;
    }
    if (this.activeForm.kind === 'incidencia' && !this.incidentDescription.trim()) {
      this.errorMessage = 'Escribe una descripción para la incidencia.';
      return;
    }

    this.clearMessages();
    this.isSaving = true;
    const request = this.activeForm.kind === 'actividad'
      ? this.daycareOperationsService.createDailyRecord(reservation.id, {
          type: this.selectedActivity,
          notes: this.activityNotes.trim() || undefined
        })
      : this.daycareOperationsService.createIncident(reservation.id, {
          severity: this.selectedSeverity,
          description: this.incidentDescription.trim()
        });

    request.subscribe({
      next: () => {
        this.successMessage = this.activeForm?.kind === 'actividad'
          ? `Actividad registrada para ${reservation.dog.name}.`
          : `Incidencia registrada para ${reservation.dog.name}.`;
        this.activeForm = null;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.getErrorMessage(error, 'No se pudo guardar la acción.');
        this.isSaving = false;
      },
      complete: () => {
        this.isSaving = false;
      }
    });
  }

  hasAlerts(reservation: DailyReservation): boolean {
    return reservation.dog.allergies.length > 0
      || reservation.dog.medications.length > 0
      || reservation.dog.incompatibilities.length > 0;
  }

  getOccupancyPercentage(occupancy: { occupied: number; capacityMax: number }): number {
    return Math.min(occupancy.occupied / occupancy.capacityMax * 100, 100);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  private getErrorMessage(error: HttpErrorResponse, fallback: string): string {
    if (error.status === 401) {
      return 'La sesión ha caducado. Vuelve a iniciar sesión.';
    }
    if (error.status === 403) {
      return 'No tienes permiso para realizar esta acción.';
    }
    if (error.status === 0) {
      return 'No se puede conectar con el backend.';
    }
    return fallback;
  }
}