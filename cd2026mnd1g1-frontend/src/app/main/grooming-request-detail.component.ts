import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  AssignGroomingAppointmentRequest,
  GroomingAppointmentDetail,
  GroomingAppointment,
  GroomingAppointmentLocation,
  GroomingAppointmentStatus,
  GroomerOption,
  GroomingRequestOrigin,
} from '../core/operational.models';

interface GroomingAssignmentDraft {
  groomerId: number | null;
  location: GroomingAppointmentLocation | '';
  date: string;
  time: string;
}

/**
 * Consulta y asignación de citas de peluquería para recepción.
 */
@Component({
  selector: 'app-grooming-request-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './grooming-request-detail.component.html',
  styleUrl: './grooming-request-detail.component.scss'
})
export class GroomingRequestDetailComponent implements OnInit {
  private static readonly assignmentDraftPrefix: string = 'reception-grooming-assignment-';

  appointment: GroomingAppointmentDetail | null = null;
  groomers: GroomerOption[] = [];
  selectedGroomerId: number | null = null;
  selectedLocation: GroomingAppointmentLocation | '' = '';
  assignmentDate: string = '';
  assignmentTime: string = '';
  isLoading: boolean = true;
  isLoadingGroomers: boolean = false;
  isAssigning: boolean = false;
  hasAccessError: boolean = false;
  errorMessage: string = '';
  assignmentError: string = '';
  assignmentSuccess: boolean = false;
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
        if (appointment.status === 'PENDIENTE_ASIGNACION' && this.canAssign) {
          this.initializeAssignment(appointment);
        }
      },
      error: (error: HttpErrorResponse) => this.handleError(error)
    });
  }

  statusLabel(status: GroomingAppointmentStatus): string {
    const labels: Record<GroomingAppointmentStatus, string> = {
      PENDIENTE_ASIGNACION: 'Pendiente de asignación',
      SOLICITADA: 'Solicitada',
      CONFIRMADA: 'Confirmada · asignada',
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

  get canAssign(): boolean {
    return this.authService.hasRole('RECEPCIONISTA');
  }

  confirmAssignment(): void {
    this.assignmentError = '';
    this.assignmentSuccess = false;
    if (this.appointment === null || !this.canAssign || this.appointment.status !== 'PENDIENTE_ASIGNACION'
        || this.selectedGroomerId === null || !this.selectedLocation
        || !this.isValidDate(this.assignmentDate) || !this.isValidTime(this.assignmentTime)
        || this.isAssigning) {
      this.assignmentError = 'Selecciona peluquero y modalidad, e indica fecha y hora válidas.';
      return;
    }

    const request: AssignGroomingAppointmentRequest = {
      groomerId: this.selectedGroomerId,
      location: this.selectedLocation,
      date: this.assignmentDate,
      time: this.assignmentTime
    };
    this.isAssigning = true;
    this.daycareOperationsService.assignReceptionGroomingAppointment(this.appointment.id, request).subscribe({
      next: (appointment: GroomingAppointmentDetail) => {
        this.appointment = appointment;
        this.isAssigning = false;
        this.assignmentSuccess = true;
        this.clearAssignmentDraft();
      },
      error: (error: HttpErrorResponse) => this.handleAssignmentError(error)
    });
  }

  cancelAssignment(): void {
    this.clearAssignmentDraft();
    this.router.navigate(['/citas-peluqueria']);
  }

  retryGroomers(): void {
    this.assignmentError = '';
    this.loadGroomers();
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
    this.clearAssignmentDraft();
    this.router.navigate(['/citas-peluqueria']);
  }

  get hasAgendaContext(): boolean {
    return this.agendaGroomerId !== null && this.agendaDate !== null;
  }

  openAgenda(): void {
    if (this.appointment === null || !this.canAssign
        || this.appointment.status !== 'PENDIENTE_ASIGNACION'
        || this.selectedGroomerId === null) {
      return;
    }
    if (!this.isValidDate(this.assignmentDate)) {
      this.assignmentError = 'Indica una fecha válida antes de consultar la agenda.';
      return;
    }
    if (!this.saveAssignmentDraft()) {
      this.assignmentError = 'No se pudo conservar la asignación para abrir la agenda. Inténtalo de nuevo.';
      return;
    }
    this.router.navigate(['/agenda-peluqueros'], {
      queryParams: {
        date: this.assignmentDate || this.appointment.date,
        groomerId: this.selectedGroomerId,
        returnTo: 'assignment',
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

  private initializeAssignment(appointment: GroomingAppointmentDetail): void {
    this.assignmentDate = appointment.date;
    this.assignmentTime = appointment.time.slice(0, 5);
    this.restoreAssignmentDraft();
    this.loadGroomers();
  }

  private loadGroomers(): void {
    this.isLoadingGroomers = true;
    this.daycareOperationsService.getReceptionGroomers().subscribe({
      next: (groomers: GroomerOption[]) => {
        this.groomers = groomers;
        this.isLoadingGroomers = false;
        if (this.selectedGroomerId !== null
            && !groomers.some((groomer) => groomer.id === this.selectedGroomerId)) {
          this.selectedGroomerId = null;
        }
      },
      error: (error: HttpErrorResponse) => {
        this.isLoadingGroomers = false;
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
        this.assignmentError = 'No se pudieron cargar los peluqueros. Puedes reintentar la consulta.';
      }
    });
  }

  private handleAssignmentError(error: HttpErrorResponse): void {
    this.isAssigning = false;
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
    if (error.status === 409) {
      this.assignmentError = 'La cita ya no está pendiente de asignación. Se actualizará su estado actual.';
      this.loadAppointment();
      return;
    }
    this.assignmentError = error.status === 404
      ? 'La cita o el peluquero ya no están disponibles en este centro.'
      : 'No se pudo asignar la cita. Comprueba los datos e inténtalo de nuevo.';
  }

  private saveAssignmentDraft(): boolean {
    if (this.appointment === null) {
      return false;
    }
    try {
      sessionStorage.setItem(
        this.assignmentDraftKey(this.appointment.id),
        JSON.stringify({
          groomerId: this.selectedGroomerId,
          location: this.selectedLocation,
          date: this.assignmentDate,
          time: this.assignmentTime
        } satisfies GroomingAssignmentDraft)
      );
      return true;
    } catch {
      return false;
    }
  }

  private restoreAssignmentDraft(): void {
    try {
      const storageKey: string = this.assignmentDraftKey(this.appointmentId);
      const storedDraft: string | null = sessionStorage.getItem(storageKey);
      if (storedDraft === null) {
        return;
      }
      sessionStorage.removeItem(storageKey);
      const draft: Partial<GroomingAssignmentDraft> = JSON.parse(storedDraft) as Partial<GroomingAssignmentDraft>;
      this.selectedGroomerId = typeof draft.groomerId === 'number'
        && Number.isSafeInteger(draft.groomerId) && draft.groomerId >= 0
        ? draft.groomerId
        : null;
      this.selectedLocation = draft.location === 'EN_INSTALACIONES' || draft.location === 'DESPLAZADO'
        ? draft.location
        : '';
      this.assignmentDate = typeof draft.date === 'string' && this.isValidDate(draft.date)
        ? draft.date
        : this.assignmentDate;
      this.assignmentTime = typeof draft.time === 'string' && this.isValidTime(draft.time)
        ? draft.time
        : this.assignmentTime;
    } catch {
      this.assignmentError = 'No se pudo recuperar el borrador de asignación. Revisa los campos.';
    }
  }

  private clearAssignmentDraft(): void {
    if (this.appointment === null) {
      return;
    }
    try {
      sessionStorage.removeItem(this.assignmentDraftKey(this.appointment.id));
    } catch {
      this.assignmentError = 'No se pudo limpiar el borrador local de asignación.';
    }
  }

  private assignmentDraftKey(appointmentId: number): string {
    return `${GroomingRequestDetailComponent.assignmentDraftPrefix}${appointmentId}`;
  }

  private isValidDate(value: string): boolean {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return false;
    }
    const parsedDate: Date = new Date(`${value}T00:00:00Z`);
    return !Number.isNaN(parsedDate.getTime()) && parsedDate.toISOString().slice(0, 10) === value;
  }

  private isValidTime(value: string): boolean {
    return /^([01]\d|2[0-3]):[0-5]\d$/.test(value);
  }

  private readId(value: string | null): number | null {
    if (value === null || !/^\d+$/.test(value)) {
      return null;
    }
    const parsedId: number = Number(value);
    return Number.isSafeInteger(parsedId) && parsedId >= 0 ? parsedId : null;
  }
}
