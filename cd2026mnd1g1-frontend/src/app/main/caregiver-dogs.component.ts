import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  CaregiverDogDetail,
  CaregiverDogSummary,
  CaregiverDailySummary,
  Incident,
  IncidentSeverity,
  DailyRecord,
  DailyRecordType,
  DogSize
} from '../core/operational.models';

/**
 * Muestra los perros asignados al cuidador para hoy y sus fichas de consulta.
 */
@Component({
  selector: 'app-caregiver-dogs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './caregiver-dogs.component.html',
  styleUrl: './caregiver-dogs.component.scss'
})
export class CaregiverDogsComponent implements OnInit, OnDestroy {
  private readonly subscriptions: Subscription = new Subscription();
  private readonly photoUrls: Map<number, string> = new Map<number, string>();
  private readonly failedPhotos: Set<number> = new Set<number>();

  page: number = 0;
  readonly pageSize: number = 50;
  totalPages: number = 0;
  totalElements: number = 0;
  dogs: CaregiverDogSummary[] = [];
  dog: CaregiverDogDetail | null = null;
  isLoading: boolean = true;
  errorMessage: string = '';
  records: DailyRecord[] = [];
  showRecordForm: boolean = false;
  isSaving: boolean = false;
  recordType: DailyRecordType | null = null;
  recordNotes: string = '';
  summary: CaregiverDailySummary | null = null;
  showSummaryForm: boolean = false;
  summaryText: string = '';
  isPublishingSummary: boolean = false;
  incidents: Incident[] = [];
  showIncidentForm: boolean = false;
  incidentSeverity: IncidentSeverity | null = null;
  incidentDescription: string = '';
  isSavingIncident: boolean = false;

  readonly dailyRecordTypes: { value: DailyRecordType; label: string }[] = [
    { value: 'COMIDA', label: 'Comida' },
    { value: 'PASEO', label: 'Paseo' },
    { value: 'JUEGO', label: 'Juego' },
    { value: 'DESCANSO', label: 'Descanso' },
    { value: 'OTRO', label: 'Otro cuidado' }
  ];

  readonly sizes: { value: DogSize; label: string }[] = [
    { value: 'PEQUENO', label: 'Pequeño' },
    { value: 'MEDIANO', label: 'Mediano' },
    { value: 'GRANDE', label: 'Grande' }
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly operationsService: DaycareOperationsService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.subscriptions.add(combineLatest([
      this.route.paramMap,
      this.route.queryParamMap
    ]).subscribe(([parameters, queryParameters]) => {
      const reservationIdValue: string | null = parameters.get('reservationId');
      const reservationId: number = reservationIdValue === null ? 0 : Number(reservationIdValue);
      const requestedPage: number = Number(queryParameters.get('page') ?? '0');
      this.page = Number.isInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;
      this.errorMessage = '';
      this.dog = null;
      this.records = [];
      this.showRecordForm = false;
      this.recordType = null;
      this.recordNotes = '';
      this.summary = null;
      this.showSummaryForm = false;
      this.summaryText = '';
      this.incidents = [];
      this.showIncidentForm = false;
      this.incidentSeverity = null;
      this.incidentDescription = '';
      if (reservationIdValue === null || !Number.isSafeInteger(reservationId) || reservationId < 0) {
        this.loadList();
      } else {
        this.loadDetail(reservationId);
      }
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.revokePhotoUrls();
  }

  get isDetail(): boolean {
    return this.dog !== null || this.route.snapshot.paramMap.has('reservationId');
  }

  getPhotoUrl(reservationId: number): string | null {
    return this.photoUrls.get(reservationId) ?? null;
  }

  hasPhotoFailed(reservationId: number): boolean {
    return this.failedPhotos.has(reservationId);
  }

  sizeLabel(size: DogSize): string {
    return this.sizes.find((option) => option.value === size)?.label ?? size;
  }

  retry(): void {
    if (this.isDetail && this.dog !== null) {
      this.loadDetail(this.dog.reservationId);
    } else {
      this.loadList();
    }
  }

  backToList(): void {
    this.router.navigate(['/mis-perros-hoy'], { queryParams: { page: this.page } });
  }

  changePage(nextPage: number): void {
    if (nextPage < 0 || nextPage >= this.totalPages) {
      return;
    }
    this.router.navigate(['/mis-perros-hoy'], { queryParams: { page: nextPage } });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  openRecordForm(): void {
    this.showRecordForm = true;
    this.errorMessage = '';
  }

  cancelRecord(): void {
    if (this.isSaving) {
      return;
    }
    this.showRecordForm = false;
    this.recordType = null;
    this.recordNotes = '';
  }

  saveRecord(): void {
    if (this.isSaving || this.dog === null || this.recordType === null) {
      return;
    }
    if (this.recordNotes.length > 2000) {
      this.errorMessage = 'Las observaciones no pueden superar 2000 caracteres.';
      return;
    }
    this.isSaving = true;
    this.errorMessage = '';
    this.subscriptions.add(this.operationsService.createCaregiverDailyRecord(
      this.dog.reservationId,
      {
        type: this.recordType,
        notes: this.recordNotes.trim() || undefined
      }
    ).subscribe({
      next: (record) => {
        this.records = [record, ...this.records];
        this.isSaving = false;
        this.showRecordForm = false;
        this.recordType = null;
        this.recordNotes = '';
      },
      error: (error: HttpErrorResponse) => {
        this.isSaving = false;
        this.handleError(error);
      }
    }));
  }

  recordTypeLabel(type: DailyRecordType): string {
    return this.dailyRecordTypes.find((item) => item.value === type)?.label ?? type;
  }

  openSummaryForm(): void {
    if (this.summary !== null) {
      return;
    }
    this.showSummaryForm = true;
    this.errorMessage = '';
  }

  cancelSummary(): void {
    if (this.isPublishingSummary) {
      return;
    }
    this.showSummaryForm = false;
    this.summaryText = '';
  }

  publishSummary(): void {
    if (this.isPublishingSummary || this.dog === null || this.summary !== null) {
      return;
    }
    const normalized: string = this.summaryText.trim();
    if (normalized.length === 0) {
      this.errorMessage = 'El resumen diario es obligatorio.';
      return;
    }
    if (normalized.length > 4000) {
      this.errorMessage = 'El resumen no puede superar 4000 caracteres.';
      return;
    }
    this.isPublishingSummary = true;
    this.errorMessage = '';
    this.subscriptions.add(this.operationsService.publishCaregiverDailySummary(
      this.dog.reservationId,
      normalized
    ).subscribe({
      next: (summary) => {
        this.summary = summary;
        this.summaryText = '';
        this.showSummaryForm = false;
        this.isPublishingSummary = false;
      },
      error: (error: HttpErrorResponse) => {
        this.isPublishingSummary = false;
        this.handleError(error);
      }
    }));
  }

  openIncidentForm(): void {
    this.showIncidentForm = true;
    this.errorMessage = '';
  }

  cancelIncident(): void {
    if (this.isSavingIncident) {
      return;
    }
    this.showIncidentForm = false;
    this.incidentSeverity = null;
    this.incidentDescription = '';
  }

  saveIncident(): void {
    if (this.isSavingIncident || this.dog === null || this.incidentSeverity === null) {
      return;
    }
    const description: string = this.incidentDescription.trim();
    if (description.length === 0) {
      this.errorMessage = 'La descripción de la incidencia es obligatoria.';
      return;
    }
    if (description.length > 2000) {
      this.errorMessage = 'La descripción no puede superar 2000 caracteres.';
      return;
    }
    this.isSavingIncident = true;
    this.errorMessage = '';
    this.subscriptions.add(this.operationsService.createCaregiverIncident(
      this.dog.reservationId,
      this.incidentSeverity,
      description
    ).subscribe({
      next: (incident) => {
        this.incidents = [incident, ...this.incidents];
        this.isSavingIncident = false;
        this.showIncidentForm = false;
        this.incidentSeverity = null;
        this.incidentDescription = '';
      },
      error: (error: HttpErrorResponse) => {
        this.isSavingIncident = false;
        this.handleError(error);
      }
    }));
  }

  private loadList(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.dog = null;
    this.subscriptions.add(this.operationsService
      .getCaregiverTodayDogs(this.page, this.pageSize)
      .subscribe({
        next: (result) => {
          this.dogs = result.items;
          this.page = result.page;
          this.totalPages = result.totalPages;
          this.totalElements = result.totalElements;
          this.isLoading = false;
          this.loadPhotos();
        },
        error: (error: HttpErrorResponse) => this.handleError(error)
      }));
  }

  private loadDetail(reservationId: number): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.subscriptions.add(this.operationsService
      .getCaregiverTodayDog(reservationId)
      .subscribe({
        next: (dog) => {
          this.dog = dog;
          this.isLoading = false;
          this.loadPhoto(reservationId);
          this.loadRecords(reservationId);
          this.loadSummary(reservationId);
          this.loadIncidents(reservationId);
        },
        error: (error: HttpErrorResponse) => this.handleError(error)
      }));
  }

  private loadPhotos(): void {
    for (const dog of this.dogs) {
      if (dog.photoAvailable) {
        this.loadPhoto(dog.reservationId);
      }
    }
  }

  private loadPhoto(reservationId: number): void {
    if (this.photoUrls.has(reservationId) || this.failedPhotos.has(reservationId)) {
      return;
    }
    this.subscriptions.add(this.operationsService
      .getCaregiverTodayDogPhoto(reservationId)
      .subscribe({
        next: (blob) => {
          this.photoUrls.set(reservationId, URL.createObjectURL(blob));
        },
        error: () => this.failedPhotos.add(reservationId)
      }));
  }

  private loadRecords(reservationId: number): void {
    this.subscriptions.add(this.operationsService
      .getCaregiverDailyRecords(reservationId)
      .subscribe({
        next: (records) => this.records = records,
        error: (error: HttpErrorResponse) => this.handleError(error)
      }));
  }

  private loadSummary(reservationId: number): void {
    this.subscriptions.add(this.operationsService
      .getCaregiverDailySummary(reservationId)
      .subscribe({
        next: (summary) => this.summary = summary,
        error: (error: HttpErrorResponse) => this.handleError(error)
      }));
  }

  private loadIncidents(reservationId: number): void {
    this.subscriptions.add(this.operationsService
      .getCaregiverIncidents(reservationId)
      .subscribe({
        next: (incidents) => this.incidents = incidents,
        error: (error: HttpErrorResponse) => this.handleError(error)
      }));
  }

  private handleError(error: HttpErrorResponse): void {
    this.isLoading = false;
    if (error.status === 401) {
      const returnUrl: string = this.router.url;
      this.authService.logout();
      this.router.navigate(['/login'], { queryParams: { returnUrl } });
      return;
    }
    this.errorMessage = error.status === 0
      ? 'No se ha podido conectar con el servidor.'
      : 'No se pudo completar la consulta.';
  }

  private revokePhotoUrls(): void {
    for (const url of this.photoUrls.values()) {
      URL.revokeObjectURL(url);
    }
  }
}
