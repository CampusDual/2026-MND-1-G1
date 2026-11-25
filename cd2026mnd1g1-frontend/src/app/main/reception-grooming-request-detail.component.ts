import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  GroomingRequestDetail,
  GroomingRequestOrigin,
  GroomingRequestStatus
} from '../core/operational.models';

type GroomingRequestAction = 'CONFIRMAR' | 'RECHAZAR';

/**
 * Consulta y permite decidir una solicitud de peluquería pendiente.
 */
@Component({
  selector: 'app-reception-grooming-request-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './reception-grooming-request-detail.component.html',
  styleUrl: './reception-grooming-request-detail.component.scss'
})
export class ReceptionGroomingRequestDetailComponent implements OnInit {
  request: GroomingRequestDetail | null = null;
  isLoading: boolean = true;
  isSubmitting: boolean = false;
  hasAccessError: boolean = false;
  errorMessage: string = '';
  actionError: string = '';
  successMessage: string = '';
  pendingAction: GroomingRequestAction | null = null;
  private requestId: number = 0;

  constructor(
    private readonly authService: AuthService,
    private readonly daycareOperationsService: DaycareOperationsService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.requestId = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(this.requestId) || this.requestId < 0) {
      this.isLoading = false;
      this.errorMessage = 'No se encontró la solicitud indicada.';
      return;
    }
    this.loadRequest();
  }

  loadRequest(): void {
    this.isLoading = true;
    this.hasAccessError = false;
    this.errorMessage = '';
    this.daycareOperationsService.getReceptionGroomingRequest(this.requestId).subscribe({
      next: (request: GroomingRequestDetail) => {
        this.request = request;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => this.handleLoadError(error)
    });
  }

  statusLabel(status: GroomingRequestStatus): string {
    const labels: Record<GroomingRequestStatus, string> = {
      SOLICITADA: 'Pendiente',
      CONFIRMADA: 'Confirmada',
      RECHAZADA: 'Rechazada',
      COMPLETADA: 'Completada',
      CANCELADA: 'Cancelada'
    };
    return labels[status];
  }

  originLabel(origin: GroomingRequestOrigin): string {
    return origin === 'B2B' ? 'Guardería cliente · B2B' : 'Guardería interna';
  }

  formatDate(date: string, time: string | null): string {
    const formattedDate: string = new Intl.DateTimeFormat('es-ES', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC'
    }).format(new Date(`${date}T12:00:00Z`));
    return time ? `${formattedDate} · ${time.slice(0, 5)}` : formattedDate;
  }

  askForDecision(action: GroomingRequestAction): void {
    this.pendingAction = action;
    this.actionError = '';
    this.successMessage = '';
  }

  cancelDecision(): void {
    if (!this.isSubmitting) {
      this.pendingAction = null;
      this.actionError = '';
    }
  }

  applyDecision(): void {
    if (!this.pendingAction || !this.request || this.request.status !== 'SOLICITADA' || this.isSubmitting) {
      return;
    }
    this.isSubmitting = true;
    this.actionError = '';
    const operation = this.pendingAction === 'CONFIRMAR'
      ? this.daycareOperationsService.confirmReceptionGroomingRequest(this.requestId)
      : this.daycareOperationsService.rejectReceptionGroomingRequest(this.requestId);
    operation.subscribe({
      next: (request: GroomingRequestDetail) => {
        this.request = request;
        this.pendingAction = null;
        this.isSubmitting = false;
        this.successMessage = request.status === 'CONFIRMADA'
          ? 'La solicitud se ha confirmado.'
          : 'La solicitud se ha rechazado.';
      },
      error: (error: HttpErrorResponse) => this.handleActionError(error)
    });
  }

  backToList(): void {
    this.router.navigate(['/solicitudes-peluqueria']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private handleLoadError(error: HttpErrorResponse): void {
    this.isLoading = false;
    if (error.status === 401) {
      this.redirectToLogin();
      return;
    }
    if (error.status === 403) {
      this.hasAccessError = true;
      return;
    }
    this.errorMessage = error.status === 404
      ? 'No se encontró la solicitud indicada.'
      : 'No se pudo cargar el detalle. Comprueba la conexión e inténtalo de nuevo.';
  }

  private handleActionError(error: HttpErrorResponse): void {
    this.isSubmitting = false;
    if (error.status === 401) {
      this.redirectToLogin();
      return;
    }
    if (error.status === 403) {
      this.hasAccessError = true;
      this.pendingAction = null;
      return;
    }
    this.actionError = error.status === 404
      ? 'La solicitud ya no está disponible en tu ámbito.'
      : error.status === 409
        ? 'La solicitud ya no está pendiente. Se ha actualizado su estado.'
        : 'No se pudo guardar la decisión. Inténtalo de nuevo.';
    if (error.status === 404 || error.status === 409) {
      this.pendingAction = null;
      this.loadRequest();
    }
  }

  private redirectToLogin(): void {
    const returnUrl: string = this.router.url;
    this.authService.logout();
    this.router.navigate(['/login'], { queryParams: { returnUrl } });
  }
}
