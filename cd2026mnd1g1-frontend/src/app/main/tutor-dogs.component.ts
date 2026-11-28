import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import { NotificationBellComponent } from '../shared/notification-bell.component';
import {
  DogSize,
  TutorAuthorizedPerson,
  TutorDogCreateRequest,
  TutorDogDetail,
  TutorDogSummary,
  TutorDogUpdateRequest
} from '../core/operational.models';

/**
 * Consulta las fichas de los perros vinculados a la cuenta autenticada del tutor.
 */
@Component({
  selector: 'app-tutor-dogs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NotificationBellComponent],
  templateUrl: './tutor-dogs.component.html',
  styleUrl: './tutor-dogs.component.scss'
})
export class TutorDogsComponent implements OnDestroy {
  private readonly routeSubscription: Subscription;
  private requestSubscriptions: Subscription = new Subscription();
  private readonly isCreateRoute: boolean;
  private readonly isEditRoute: boolean;
  private readonly photoUrls: Map<number, string> = new Map<number, string>();
  private readonly failedPhotos: Set<number> = new Set<number>();
  page: number = 0;
  private currentDogId: number | null = null;
  private etag: string | null = null;
  private readonly pageSize: number = 50;

  dogs: TutorDogSummary[] = [];
  dog: TutorDogDetail | null = null;
  loading: boolean = true;
  errorMessage: string = '';
  configurationMessage: string = '';
  createErrorMessage: string = '';
  formErrorMessage: string = '';
  photoErrorMessage: string = '';
  dogName: string = '';
  breed: string = '';
  size: DogSize | '' = '';
  birthDate: string = '';
  allergyRows: string[] = [];
  medicationRows: { medication: string; dosage: string; instructions: string }[] = [];
  authorizedPersonRows: { id: number | null; name: string; phone: string }[] = [];
  removeExistingPhoto: boolean = false;
  existingAuthorizedPersons: TutorAuthorizedPerson[] = [];
  selectedPhoto: File | null = null;
  photoPreviewUrl: string | null = null;
  isSaving: boolean = false;
  retryRequired: boolean = false;
  private idempotencyKey: string | null = null;
  readonly sizes: { value: DogSize; label: string }[] = [
    { value: 'PEQUENO', label: 'Pequeño' },
    { value: 'MEDIANO', label: 'Mediano' },
    { value: 'GRANDE', label: 'Grande' }
  ];
  readonly today: string = this.getTodayDateInCenterZone();
  totalPages: number = 0;
  totalElements: number = 0;

  constructor(
    private readonly authService: AuthService,
    private readonly operationsService: DaycareOperationsService,
    private readonly router: Router,
    route: ActivatedRoute
  ) {
    this.isCreateRoute = route.snapshot.routeConfig?.path === 'mis-perros/nuevo';
    this.isEditRoute = route.snapshot.routeConfig?.path === 'mis-perros/:id/editar';
    this.routeSubscription = combineLatest([route.paramMap, route.queryParamMap]).subscribe(
      ([parameters, queryParameters]) => {
        const dogIdValue: string | null = parameters.get('id');
        this.currentDogId = dogIdValue === null ? null : Number(dogIdValue);
        const requestedPage: number = Number(queryParameters.get('page') ?? '0');
        this.page = Number.isInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;
        this.loadCurrentView();
      }
    );
  }

  ngOnDestroy(): void {
    this.routeSubscription.unsubscribe();
    this.requestSubscriptions.unsubscribe();
    this.revokePhotoUrls();
    this.clearPhotoPreview();
  }

  get isDetail(): boolean {
    return this.currentDogId !== null;
  }

  get isCreate(): boolean {
    return this.isCreateRoute;
  }

  get isEdit(): boolean {
    return this.isEditRoute;
  }

  get isDogForm(): boolean {
    return this.isCreate || this.isEdit;
  }

  getPhotoUrl(dogId: number): string | null {
    return this.photoUrls.get(dogId) ?? null;
  }

  getDuplicateReference(dog: TutorDogSummary): string | null {
    const normalizedName: string = dog.name.toLocaleLowerCase('es-ES');
    const hasDuplicate: boolean = this.dogs.some(
      (item: TutorDogSummary) => item.id !== dog.id
        && item.name.toLocaleLowerCase('es-ES') === normalizedName
    );
    return hasDuplicate ? `Ficha ${dog.id}` : null;
  }

  retry(): void {
    this.loadCurrentView();
  }

  addAllergy(): void {
    this.allergyRows.push('');
  }

  removeAllergy(index: number): void {
    this.allergyRows.splice(index, 1);
  }

  addMedication(): void {
    this.medicationRows.push({ medication: '', dosage: '', instructions: '' });
  }

  removeMedication(index: number): void {
    this.medicationRows.splice(index, 1);
  }

  addAuthorizedPerson(): void {
    this.authorizedPersonRows.push({ id: null, name: '', phone: '' });
  }

  removeAuthorizedPerson(index: number): void {
    if (this.authorizedPersonRows[index]?.id !== null
      && !window.confirm('¿Quieres revocar esta autorización para todos tus perros actuales y futuros?')) {
      return;
    }
    this.authorizedPersonRows.splice(index, 1);
  }

  selectPhoto(event: Event): void {
    const input: HTMLInputElement = event.target as HTMLInputElement;
    const file: File | undefined = input.files?.[0];
    this.clearPhotoPreview();
    this.photoErrorMessage = '';
    this.selectedPhoto = file ?? null;
    if (file !== undefined) {
      this.removeExistingPhoto = false;
    }
    if (file === undefined) {
      return;
    }
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      this.photoErrorMessage = 'Selecciona una imagen JPEG o PNG.';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.photoErrorMessage = 'La foto no puede superar 5 MB.';
      return;
    }
    this.photoPreviewUrl = URL.createObjectURL(file);
  }

  removePhoto(): void {
    this.selectedPhoto = null;
    this.photoErrorMessage = '';
    this.clearPhotoPreview();
    this.removeExistingPhoto = this.isEdit && Boolean(this.dog?.photoAvailable);
  }

  cancelPhotoSelection(): void {
    this.selectedPhoto = null;
    this.photoErrorMessage = '';
    this.clearPhotoPreview();
    this.removeExistingPhoto = false;
  }

  keepCurrentPhoto(): void {
    this.removeExistingPhoto = false;
  }

  cancelCreate(): void {
    this.router.navigate(['/mis-perros']);
  }

  submitCreate(): void {
    if (this.isEdit) {
      this.submitEdit();
      return;
    }
    if (this.isSaving || this.retryRequired) {
      if (this.retryRequired) {
        this.sendCreateRequest();
      }
      return;
    }
    this.createErrorMessage = '';
    this.formErrorMessage = this.validateCreateForm();
    if (this.formErrorMessage || this.photoErrorMessage) {
      return;
    }
    if (this.hasRepeatedAuthorizedPerson()
      && !window.confirm('Hay una persona con el mismo nombre y teléfono en tus autorizaciones o en este formulario. ¿Quieres añadirla igualmente como un nuevo registro?')) {
      return;
    }
    this.idempotencyKey = crypto.randomUUID();
    this.sendCreateRequest();
  }

  changePage(nextPage: number): void {
    if (nextPage < 0 || nextPage >= this.totalPages) {
      return;
    }
    this.router.navigate(['/mis-perros'], { queryParams: { page: nextPage } });
  }

  backToList(): void {
    this.router.navigate(['/mis-perros'], { queryParams: { page: this.page } });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  onPhotoError(dogId: number): void {
    this.failedPhotos.add(dogId);
    this.revokePhotoUrl(dogId);
  }

  sizeLabel(size: string): string {
    switch (size) {
      case 'PEQUENO':
        return 'Pequeño';
      case 'MEDIANO':
        return 'Mediano';
      case 'GRANDE':
        return 'Grande';
      default:
        return 'Tamaño no indicado';
    }
  }

  private loadCurrentView(): void {
    this.requestSubscriptions.unsubscribe();
    this.requestSubscriptions = new Subscription();
    this.revokePhotoUrls();
    this.failedPhotos.clear();
    this.dogs = [];
    this.dog = null;
    this.errorMessage = '';
    this.configurationMessage = '';
    this.etag = null;
    this.removeExistingPhoto = false;
    this.loading = true;

    if (this.isCreate) {
      this.loadAuthorizedPersonsForCreate();
      return;
    }
    if (this.currentDogId !== null) {
      if (!Number.isSafeInteger(this.currentDogId) || this.currentDogId < 0) {
        this.loading = false;
        this.errorMessage = 'No se ha encontrado la ficha solicitada.';
        return;
      }
      this.loadDog(this.currentDogId);
      return;
    }
    this.loadDogs();
  }

  private loadAuthorizedPersonsForCreate(): void {
    this.requestSubscriptions.add(this.operationsService.getMyAuthorizedPersons().subscribe({
      next: (persons: TutorAuthorizedPerson[]) => {
        this.existingAuthorizedPersons = persons;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => this.handleError(error)
    }));
  }

  private loadDogs(): void {
    this.requestSubscriptions.add(this.operationsService.getMyDogs(this.page, this.pageSize).subscribe({
      next: (result) => {
        this.dogs = result.items;
        this.page = result.page;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;
        this.loading = false;
        result.items.filter((item: TutorDogSummary) => item.photoAvailable)
          .forEach((item: TutorDogSummary) => this.loadPhoto(item.id));
      },
      error: (error: HttpErrorResponse) => this.handleError(error)
    }));
  }

  private loadDog(dogId: number): void {
    this.requestSubscriptions.add(this.operationsService.getMyDog(dogId).subscribe({
      next: (result) => {
        this.dog = result.dog;
        this.etag = result.etag;
        if (this.isEdit) {
          this.populateEditForm(result.dog);
        }
        this.loading = false;
        if (result.dog.photoAvailable) {
          this.loadPhoto(result.dog.id);
        }
      },
      error: (error: HttpErrorResponse) => this.handleError(error)
    }));
  }

  private loadPhoto(dogId: number): void {
    if (this.failedPhotos.has(dogId)) {
      return;
    }
    this.requestSubscriptions.add(this.operationsService.getMyDogPhoto(dogId).subscribe({
      next: (image: Blob) => this.photoUrls.set(dogId, URL.createObjectURL(image)),
      error: (error: HttpErrorResponse) => {
        this.failedPhotos.add(dogId);
        if (error.status === 401) {
          this.handleError(error);
        }
      }
    }));
  }

  private handleError(error: HttpErrorResponse): void {
    this.loading = false;
    if (error.status === 401) {
      const returnUrl: string = this.router.url;
      this.authService.logout();
      this.router.navigate(['/login'], { queryParams: { returnUrl } });
      return;
    }
    const errorCode: string | undefined = error.error?.code;
    if (error.status === 409 && errorCode === 'TUTOR_ACCOUNT_NOT_LINKED') {
      this.configurationMessage =
        'Tu cuenta todavía no está vinculada a una ficha de tutor. Contacta con recepción para configurarla.';
      return;
    }
    if (error.status === 409 && errorCode === 'TUTOR_ACCOUNT_INCONSISTENT') {
      this.configurationMessage =
        'No se puede consultar el portal por una incidencia de configuración. Contacta con soporte.';
      return;
    }
    this.errorMessage = error.status === 404 && this.isDetail
      ? 'No se ha encontrado la ficha solicitada.'
      : 'No se han podido cargar tus perros. Comprueba la conexión e inténtalo de nuevo.';
  }

  private validateCreateForm(): string {
    if (!this.dogName.trim()) {
      return 'El nombre del perro es obligatorio.';
    }
    if (this.dogName.trim().length > 120 || this.breed.trim().length > 120) {
      return 'El nombre y la raza admiten hasta 120 caracteres.';
    }
    if (!this.size) {
      return 'Selecciona el tamaño del perro.';
    }
    if (this.birthDate && this.birthDate > this.today) {
      return 'La fecha de nacimiento no puede ser futura.';
    }
    if (this.allergyRows.some((description: string) => !description.trim() || description.trim().length > 1000)) {
      return 'Cada alergia debe incluir una descripción de hasta 1000 caracteres.';
    }
    if (this.medicationRows.some((item) => !item.medication.trim()
      || item.medication.trim().length > 160
      || item.dosage.trim().length > 255
      || !item.instructions.trim()
      || item.instructions.trim().length > 1000)) {
      return 'Cada pauta de medicación necesita medicamento y pauta; revisa también los límites de longitud.';
    }
    if (this.authorizedPersonRows.some((person) => {
      const hasName: boolean = Boolean(person.name.trim());
      const hasPhone: boolean = Boolean(person.phone.trim());
      const existingPersonWasCleared: boolean = person.id !== null && !hasName && !hasPhone;
      return existingPersonWasCleared || hasName !== hasPhone
        || person.name.trim().length > 120 || person.phone.trim().length > 30;
    })) {
      return 'Cada persona autorizada debe incluir nombre y teléfono (hasta 120 y 30 caracteres).';
    }
    return '';
  }

  private populateEditForm(dog: TutorDogDetail): void {
    this.dogName = dog.name;
    this.breed = dog.breed ?? '';
    this.size = dog.size;
    this.birthDate = dog.birthDate ?? '';
    this.allergyRows = dog.allergies
      .filter((allergy) => allergy.declaredByTutor)
      .map((allergy) => allergy.description);
    this.medicationRows = dog.medications
      .filter((medication) => medication.declaredByTutor)
      .map((medication) => ({
        medication: medication.medication,
        dosage: medication.dosage ?? '',
        instructions: medication.instructions
      }));
    this.authorizedPersonRows = dog.authorizedPersons.map((person) => ({
      id: person.id,
      name: person.name,
      phone: person.phone
    }));
    this.existingAuthorizedPersons = dog.authorizedPersons;
  }

  private submitEdit(): void {
    this.formErrorMessage = this.validateCreateForm();
    if (this.formErrorMessage || this.photoErrorMessage || this.isSaving) {
      return;
    }
    if (this.hasRepeatedAuthorizedPerson()
      && !window.confirm('Hay una persona con el mismo nombre y teléfono en tus autorizaciones o en este formulario. ¿Quieres mantenerla como un registro independiente?')) {
      return;
    }
    if (this.currentDogId === null || this.size === '' || this.etag === null) {
      this.formErrorMessage = 'No se pudo cargar la versión actual de la ficha. Vuelve a abrirla antes de guardar.';
      return;
    }
    const request: TutorDogUpdateRequest = {
      name: this.dogName.trim(),
      breed: this.breed.trim() || null,
      size: this.size,
      birthDate: this.birthDate || null,
      allergies: this.allergyRows.map((description) => ({ description: description.trim() })),
      medications: this.medicationRows.map((item) => ({
        medication: item.medication.trim(),
        dosage: item.dosage.trim() || null,
        instructions: item.instructions.trim()
      })),
      authorizedPersons: this.authorizedPersonRows
        .filter((person) => person.name.trim() || person.phone.trim())
        .map((person) => ({
        id: person.id,
        name: person.name.trim(),
        phone: person.phone.trim()
        })),
      removePhoto: this.removeExistingPhoto
    };
    this.isSaving = true;
    this.operationsService.updateMyDog(
      this.currentDogId,
      request,
      this.selectedPhoto,
      this.etag
    ).subscribe({
      next: (result) => {
        this.isSaving = false;
        this.router.navigate(['/mis-perros', result.dog.id]);
      },
      error: (error: HttpErrorResponse) => {
        this.isSaving = false;
        if (error.status === 401) {
          this.handleError(error);
          return;
        }
        this.formErrorMessage = error.status === 409 || error.status === 412
          ? 'La ficha ha cambiado mientras la editabas. Tus datos siguen en el formulario; cancela y vuelve a abrir la ficha para cargar la versión actual.'
          : 'No se pudo guardar la ficha. Tus cambios siguen en el formulario; revisa los datos e inténtalo de nuevo.';
      }
    });
  }

  private sendCreateRequest(): void {
    if (this.idempotencyKey === null || this.size === '') {
      return;
    }
    const request: TutorDogCreateRequest = {
      name: this.dogName.trim(),
      breed: this.breed.trim() || null,
      size: this.size,
      birthDate: this.birthDate || null,
      allergies: this.allergyRows.map((description: string) => ({ description: description.trim() })),
      medications: this.medicationRows.map((item) => ({
        medication: item.medication.trim(),
        dosage: item.dosage.trim() || null,
        instructions: item.instructions.trim()
      })),
      authorizedPersons: this.authorizedPersonRows
        .filter((person) => person.name.trim() || person.phone.trim())
        .map((person) => ({ name: person.name.trim(), phone: person.phone.trim() }))
    };
    this.isSaving = true;
    this.createErrorMessage = '';
    this.operationsService.createMyDog(request, this.selectedPhoto, this.idempotencyKey).subscribe({
      next: (dog: TutorDogDetail) => {
        this.isSaving = false;
        this.retryRequired = false;
        this.router.navigate(['/mis-perros', dog.id]);
      },
      error: (error: HttpErrorResponse) => {
        this.isSaving = false;
        if (error.status === 401) {
          this.handleError(error);
          return;
        }
        if (error.status === 0 || error.status >= 500) {
          this.retryRequired = true;
          this.createErrorMessage = error.status === 0
            ? 'No se pudo confirmar si el alta se guardó por un problema de conexión. Reintenta con los mismos datos para evitar duplicados.'
            : 'El servidor encontró un error al procesar el alta. Reintenta con los mismos datos para evitar duplicados; el servidor registró el detalle técnico.';
          return;
        }
        this.retryRequired = false;
        this.idempotencyKey = null;
        const serverMessage: unknown = error.error?.message;
        this.createErrorMessage = typeof serverMessage === 'string'
          ? serverMessage
          : 'No se pudo guardar el perro. Revisa los datos e inténtalo de nuevo.';
      }
    });
  }

  private hasRepeatedAuthorizedPerson(): boolean {
    const normalizeName = (value: string): string => value.trim().replace(/\s+/g, ' ').toLocaleLowerCase('es-ES');
    const normalizePhone = (value: string): string => value.replace(/[\s().-]/g, '');
    const seen: Set<string> = new Set();
    for (const person of this.authorizedPersonRows) {
      if (!person.name.trim() || !person.phone.trim()) {
        continue;
      }
      const key: string = `${normalizeName(person.name)}|${normalizePhone(person.phone)}`;
      const repeatsExisting: boolean = this.existingAuthorizedPersons.some((existing) =>
        existing.id !== person.id
        && `${normalizeName(existing.name)}|${normalizePhone(existing.phone)}` === key
      );
      if (seen.has(key) || repeatsExisting) {
        return true;
      }
      seen.add(key);
    }
    return false;
  }

  private clearPhotoPreview(): void {
    if (this.photoPreviewUrl !== null) {
      URL.revokeObjectURL(this.photoPreviewUrl);
      this.photoPreviewUrl = null;
    }
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

  private revokePhotoUrls(): void {
    Array.from(this.photoUrls.keys()).forEach((dogId: number) => this.revokePhotoUrl(dogId));
  }

  private revokePhotoUrl(dogId: number): void {
    const photoUrl: string | undefined = this.photoUrls.get(dogId);
    if (photoUrl !== undefined) {
      URL.revokeObjectURL(photoUrl);
      this.photoUrls.delete(dogId);
    }
  }
}
