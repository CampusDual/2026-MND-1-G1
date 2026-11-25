import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  GroomingRequest,
  GroomingRequestOrigin,
  GroomingRequestPage,
  GroomingRequestStatus
} from '../core/operational.models';

/**
 * Lista solicitudes de peluquería recibidas por el centro de recepción.
 */
@Component({
  selector: 'app-reception-grooming-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reception-grooming-requests.component.html',
  styleUrl: './reception-grooming-requests.component.scss'
})
export class ReceptionGroomingRequestsComponent implements OnInit {
  private readonly pageSize: number = 25;

  readonly statuses: GroomingRequestStatus[] = [
    'SOLICITADA', 'CONFIRMADA', 'RECHAZADA', 'COMPLETADA', 'CANCELADA'
  ];
  origin: GroomingRequestOrigin | '' = '';
  status: GroomingRequestStatus | '' = '';
  fromDate: string = '';
  toDate: string = '';
  requests: GroomingRequest[] = [];
  currentPage: number = 0;
  totalPages: number = 0;
  totalElements: number = 0;
  isLoading: boolean = true;
  hasAccessError: boolean = false;
  errorMessage: string = '';

  constructor(
    private readonly authService: AuthService,
    private readonly daycareOperationsService: DaycareOperationsService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.isLoading = true;
    this.hasAccessError = false;
    this.errorMessage = '';
    this.daycareOperationsService.getReceptionGroomingRequests(
      { origin: this.origin, status: this.status, from: this.fromDate, to: this.toDate },
      this.currentPage,
      this.pageSize
    ).subscribe({
      next: (page: GroomingRequestPage) => {
        this.requests = page.items;
        this.currentPage = page.page;
        this.totalPages = page.totalPages;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => this.handleError(error)
    });
  }

  applyFilters(): void {
    if (!this.isValidDateRange()) {
      return;
    }
    this.currentPage = 0;
    this.loadRequests();
  }

  changePage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.currentPage = page;
      this.loadRequests();
    }
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
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC'
    }).format(new Date(`${date}T12:00:00Z`));
    return time ? `${formattedDate} · ${time.slice(0, 5)}` : formattedDate;
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
    this.errorMessage = error.status === 400
      ? 'Los filtros indicados no tienen un formato válido.'
      : 'No se pudieron cargar las solicitudes. Comprueba la conexión e inténtalo de nuevo.';
  }

  private isValidDateRange(): boolean {
    if ((this.fromDate && !this.isValidDate(this.fromDate))
        || (this.toDate && !this.isValidDate(this.toDate))) {
      this.errorMessage = 'La fecha indicada no es válida.';
      return false;
    }
    if (this.fromDate && this.toDate && this.fromDate > this.toDate) {
      this.errorMessage = 'La fecha inicial no puede ser posterior a la fecha final.';
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
