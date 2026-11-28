import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  CreateGroomingAppointmentRequest,
  GroomingDogOption,
  GroomingDogOptionPage,
  GroomingServiceOption
} from '../core/operational.models';

/**
 * Formulario de recepción para crear una cita interna de peluquería.
 */
@Component({
  selector: 'app-grooming-appointment-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './grooming-appointment-create.component.html',
  styleUrl: './grooming-appointment-create.component.scss'
})
export class GroomingAppointmentCreateComponent implements OnInit {
  private static readonly draftStorageKey: string = 'reception-grooming-appointment-draft';
  private readonly dogPageSize: number = 50;
  private restoredDogOption: GroomingDogOption | null = null;

  dogs: GroomingDogOption[] = [];
  services: GroomingServiceOption[] = [];
  dogSearch: string = '';
  selectedDogId: number | null = null;
  selectedServiceId: number | null = null;
  date: string = '';
  time: string = '';
  notes: string = '';
  dogPage: number = 0;
  dogTotalPages: number = 0;
  isLoadingDogs: boolean = true;
  isLoadingServices: boolean = true;
  isSubmitting: boolean = false;
  hasAccessError: boolean = false;
  dogsError: string = '';
  servicesError: string = '';
  submitError: string = '';

  constructor(
    private readonly authService: AuthService,
    private readonly daycareOperationsService: DaycareOperationsService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.restoreDraft();
    this.loadDogs(0);
    this.loadServices();
  }

  searchDogs(): void {
    this.loadDogs(0);
  }

  reloadServices(): void {
    this.loadServices();
  }

  changeDogPage(page: number): void {
    if (page >= 0 && page < this.dogTotalPages && page !== this.dogPage) {
      this.loadDogs(page);
    }
  }

  submit(): void {
    this.submitError = '';
    if (this.selectedDogId === null || this.selectedServiceId === null
        || !this.isValidDate(this.date) || !this.isValidTime(this.time)) {
      this.submitError = 'Selecciona un perro y un servicio, e indica una fecha y una hora válidas.';
      return;
    }

    const request: CreateGroomingAppointmentRequest = {
      dogId: this.selectedDogId,
      serviceId: this.selectedServiceId,
      date: this.date,
      time: this.time,
      notes: this.notes.trim() || null
    };
    this.isSubmitting = true;
    this.daycareOperationsService.createReceptionGroomingAppointment(request).subscribe({
      next: (appointment) => {
        this.isSubmitting = false;
        this.clearDraft();
        this.router.navigate(['/citas-peluqueria', appointment.id]);
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting = false;
        this.handleSubmitError(error);
      }
    });
  }

  cancel(): void {
    this.clearDraft();
    this.router.navigate(['/citas-peluqueria']);
  }

  openAgenda(): void {
    try {
      sessionStorage.setItem(GroomingAppointmentCreateComponent.draftStorageKey, JSON.stringify({
        selectedDogId: this.selectedDogId,
        selectedDogOption: this.dogs.find((dog) => dog.id === this.selectedDogId) || null,
        selectedServiceId: this.selectedServiceId,
        date: this.date,
        time: this.time,
        notes: this.notes,
        dogSearch: this.dogSearch
      }));
      this.router.navigate(['/agenda-peluqueros'], {
        queryParams: {
          date: this.date || new Date().toISOString().slice(0, 10),
          returnTo: 'create'
        }
      });
    } catch {
      this.submitError = 'No se pudo conservar el formulario para abrir la agenda. Inténtalo de nuevo.';
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private loadDogs(page: number): void {
    this.isLoadingDogs = true;
    this.dogsError = '';
    this.daycareOperationsService.getReceptionGroomingDogs(this.dogSearch, page, this.dogPageSize).subscribe({
      next: (result: GroomingDogOptionPage) => {
        const selectedDogStillAvailable: boolean = this.restoredDogOption !== null
          && this.restoredDogOption.id === this.selectedDogId
          && !result.items.some((dog) => dog.id === this.restoredDogOption?.id);
        this.dogs = selectedDogStillAvailable && this.restoredDogOption !== null
          ? [this.restoredDogOption, ...result.items]
          : result.items;
        this.restoredDogOption = null;
        this.dogPage = result.page;
        this.dogTotalPages = result.totalPages;
        this.isLoadingDogs = false;
      },
      error: (error: HttpErrorResponse) => {
        this.isLoadingDogs = false;
        this.handleOptionError(error, 'No se pudieron cargar los perros internos.', 'dogs');
      }
    });
  }

  private loadServices(): void {
    this.isLoadingServices = true;
    this.servicesError = '';
    this.daycareOperationsService.getReceptionGroomingServices().subscribe({
      next: (services: GroomingServiceOption[]) => {
        this.services = services;
        this.isLoadingServices = false;
      },
      error: (error: HttpErrorResponse) => {
        this.isLoadingServices = false;
        this.handleOptionError(error, 'No se pudieron cargar los servicios disponibles.', 'services');
      }
    });
  }

  private handleOptionError(
    error: HttpErrorResponse,
    message: string,
    target: 'dogs' | 'services'
  ): void {
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
    if (target === 'dogs') {
      this.dogsError = message;
    } else {
      this.servicesError = message;
    }
  }

  private handleSubmitError(error: HttpErrorResponse): void {
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
    if (error.status === 404) {
      this.submitError = 'El perro o el servicio ya no está disponible. Actualiza las opciones e inténtalo de nuevo.';
      return;
    }
    this.submitError = error.status === 400
      ? 'Revisa los campos obligatorios y el formato de la fecha y la hora.'
      : 'No se pudo guardar la cita. El formulario conserva los datos para que puedas intentarlo de nuevo.';
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

  private restoreDraft(): void {
    try {
      const storedDraft: string | null = sessionStorage.getItem(
        GroomingAppointmentCreateComponent.draftStorageKey
      );
      if (storedDraft === null) {
        return;
      }
      this.clearDraft();
      const draft: Partial<GroomingAppointmentDraft> = JSON.parse(storedDraft) as Partial<GroomingAppointmentDraft>;
      this.selectedDogId = this.validNullableId(draft.selectedDogId);
      this.selectedServiceId = this.validNullableId(draft.selectedServiceId);
      this.date = typeof draft.date === 'string' ? draft.date : '';
      this.time = typeof draft.time === 'string' ? draft.time : '';
      this.notes = typeof draft.notes === 'string' ? draft.notes : '';
      this.dogSearch = typeof draft.dogSearch === 'string' ? draft.dogSearch : '';
      this.restoredDogOption = this.validDogOption(draft.selectedDogOption, this.selectedDogId);
    } catch {
      this.submitError = 'No se pudo restaurar el formulario anterior. Comprueba los campos antes de continuar.';
    }
  }

  private validNullableId(value: unknown): number | null {
    return typeof value === 'number' && Number.isSafeInteger(value) && value >= 0 ? value : null;
  }

  private clearDraft(): void {
    sessionStorage.removeItem(GroomingAppointmentCreateComponent.draftStorageKey);
  }

  private validDogOption(value: unknown, selectedDogId: number | null): GroomingDogOption | null {
    if (typeof value !== 'object' || value === null || selectedDogId === null) {
      return null;
    }
    const option = value as Partial<GroomingDogOption>;
    if (option.id !== selectedDogId || typeof option.name !== 'string'
        || (option.tutorName !== null && typeof option.tutorName !== 'string')) {
      return null;
    }
    return {
      id: option.id,
      name: option.name,
      tutorName: option.tutorName
    };
  }
}

interface GroomingAppointmentDraft {
  selectedDogId: number | null;
  selectedDogOption: GroomingDogOption | null;
  selectedServiceId: number | null;
  date: string;
  time: string;
  notes: string;
  dogSearch: string;
}
