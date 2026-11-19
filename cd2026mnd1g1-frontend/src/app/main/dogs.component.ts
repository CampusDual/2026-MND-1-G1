import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import {
  AuthorizedPerson,
  Dog,
  DogCreateRequest,
  DogDetailVersionedResponse,
  DogSize,
  DogUpdateRequest,
  DogVersionedResponse,
  Tutor
} from '../core/operational.models';

type DogScreen = 'list' | 'create' | 'detail' | 'edit';
type MatchConfirmation = 'confirmed' | 'cancelled' | 'not-match';

interface DogForm {
  tutorId: number | null;
  name: string;
  breed: string;
  size: DogSize | null;
  birthDate: string;
}

/**
 * Pantallas de búsqueda, ficha, alta y edición básica de perros.
 */
@Component({
  selector: 'app-dogs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dogs.component.html',
  styleUrls: ['./dogs.component.scss']
})
export class DogsComponent implements OnInit, OnDestroy {
  private readonly subscriptions: Subscription = new Subscription();
  private loadSubscription: Subscription | null = null;
  private tutorLoadSubscription: Subscription | null = null;
  private dogEtag: string = '';
  private screen: DogScreen = 'list';

  readonly sizes: { value: DogSize; label: string }[] = [
    { value: 'PEQUENO', label: 'Pequeño' },
    { value: 'MEDIANO', label: 'Mediano' },
    { value: 'GRANDE', label: 'Grande' }
  ];
  readonly today: string = this.getToday();

  search: string = '';
  page: number = 0;
  pageSize: number = 50;
  totalPages: number = 0;
  totalElements: number = 0;
  dogs: Dog[] = [];
  dog: Dog | null = null;
  authorizedPersons: AuthorizedPerson[] = [];
  tutors: Tutor[] = [];
  selectedTutor: Tutor | null = null;
  tutorSearch: string = '';
  form: DogForm = this.emptyForm();
  isLoading: boolean = true;
  isTutorLoading: boolean = false;
  isSubmitting: boolean = false;
  errorMessage: string = '';
  tutorErrorMessage: string = '';
  hasAccessError: boolean = false;

  constructor(
    private readonly authService: AuthService,
    private readonly daycareOperationsService: DaycareOperationsService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.subscriptions.add(combineLatest([
      this.route.data,
      this.route.queryParamMap
    ]).subscribe(([data, parameters]) => {
      this.screen = data['screen'] as DogScreen;
      this.search = parameters.get('search') ?? '';
      this.page = Number(parameters.get('page') ?? '0');
      this.errorMessage = '';
      this.tutorErrorMessage = '';
      this.hasAccessError = false;
      this.dog = null;
      this.authorizedPersons = [];

      if (this.screen === 'list') {
        this.loadDogs();
      } else if (this.screen === 'create') {
        this.form = this.emptyForm();
        this.selectedTutor = null;
        this.tutors = [];
        this.isLoading = false;
        this.loadTutors();
      } else {
        this.loadDog();
      }
    }));
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
    this.tutorLoadSubscription?.unsubscribe();
    this.subscriptions.unsubscribe();
  }

  get isList(): boolean {
    return this.screen === 'list';
  }

  get isCreate(): boolean {
    return this.screen === 'create';
  }

  get isEdit(): boolean {
    return this.screen === 'edit';
  }

  get isDetail(): boolean {
    return this.screen === 'detail';
  }

  loadDogs(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.hasAccessError = false;
    this.dogs = [];
    this.loadSubscription?.unsubscribe();
    this.loadSubscription = this.daycareOperationsService
      .getDogs(this.search, this.page, this.pageSize)
      .subscribe({
        next: (result) => {
          this.dogs = result.items;
          this.page = result.page;
          this.totalPages = result.totalPages;
          this.totalElements = result.totalElements;
          this.isLoading = false;
        },
        error: (error: HttpErrorResponse) => this.handleError(error)
      });
  }

  searchDogs(): void {
    this.page = 0;
    this.updateListUrl();
  }

  changePage(nextPage: number): void {
    if (nextPage < 0 || nextPage >= this.totalPages) {
      return;
    }
    this.page = nextPage;
    this.updateListUrl();
  }

  searchTutors(): void {
    this.loadTutors();
  }

  selectTutor(tutorId: number | null): void {
    this.form.tutorId = tutorId;
    this.selectedTutor = this.tutors.find((tutor) => tutor.id === tutorId) ?? null;
  }

  tutorOptions(): Tutor[] {
    if (this.form.tutorId === null
        || this.tutors.some((tutor) => tutor.id === this.form.tutorId)
        || this.selectedTutor === null) {
      return this.tutors;
    }
    return [...this.tutors, this.selectedTutor];
  }

  saveDog(): void {
    if (this.isSubmitting) {
      return;
    }
    if (this.form.name.trim().length === 0) {
      this.errorMessage = 'El nombre del perro es obligatorio.';
      return;
    }
    if (this.isCreate && this.form.tutorId === null) {
      this.errorMessage = 'Debes seleccionar un tutor.';
      return;
    }
    if (this.form.size === null) {
      this.errorMessage = 'Debes seleccionar un tamaño.';
      return;
    }
    this.isSubmitting = true;
    this.errorMessage = '';
    if (this.isCreate) {
      this.createDog(this.toCreateRequest(false));
      return;
    }
    if (this.dog === null) {
      this.isSubmitting = false;
      this.errorMessage = 'No se ha podido cargar el perro que quieres editar.';
      return;
    }
    this.updateDog(this.toUpdateRequest(false));
  }

  cancelForm(): void {
    this.backToList();
  }

  backToList(): void {
    this.router.navigate(['/perros'], { queryParams: this.listQueryParams() });
  }

  editDog(): void {
    if (this.dog !== null) {
      this.router.navigate(['/perros', this.dog.id, 'editar'], {
        queryParams: this.listQueryParams()
      });
    }
  }

  retry(): void {
    if (this.screen === 'list') {
      this.loadDogs();
    } else {
      this.loadDog();
    }
  }

  reloadDog(): void {
    if (this.dog === null || !window.confirm(
      'Al recargar se sustituirán los cambios que aún no has guardado. ¿Quieres continuar?'
    )) {
      return;
    }
    this.loadDog();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  sizeLabel(size: DogSize): string {
    return this.sizes.find((option) => option.value === size)?.label ?? size;
  }

  private loadTutors(): void {
    this.isTutorLoading = true;
    this.tutorErrorMessage = '';
    this.tutorLoadSubscription?.unsubscribe();
    this.tutorLoadSubscription = this.daycareOperationsService
      .getTutors(this.tutorSearch, 0, 100)
      .subscribe({
        next: (result) => {
          this.tutors = result.items;
          if (this.form.tutorId !== null) {
            this.selectedTutor = this.tutors.find((tutor) => tutor.id === this.form.tutorId)
              ?? this.selectedTutor;
          }
          this.isTutorLoading = false;
        },
        error: (error: HttpErrorResponse) => {
          this.isTutorLoading = false;
          this.handleError(error, true);
        }
      });
  }

  private loadDog(): void {
    const dogId: number = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(dogId) || dogId < 0) {
      this.isLoading = false;
      this.errorMessage = 'El identificador del perro no es válido.';
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.hasAccessError = false;
    this.loadSubscription?.unsubscribe();
    this.loadSubscription = this.daycareOperationsService.getDog(dogId).subscribe({
      next: (result: DogDetailVersionedResponse) => {
        this.dog = result.dog;
        this.authorizedPersons = result.authorizedPersons;
        this.dogEtag = result.etag;
        this.form = {
          tutorId: result.dog.tutorId,
          name: result.dog.name,
          breed: result.dog.breed ?? '',
          size: result.dog.size,
          birthDate: result.dog.birthDate ?? ''
        };
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => this.handleError(error)
    });
  }

  private createDog(request: DogCreateRequest): void {
    this.subscriptions.add(this.daycareOperationsService.createDog(request).subscribe({
      next: (created) => {
        this.isSubmitting = false;
        this.router.navigate(['/perros', created.id], {
          queryParams: this.listQueryParams()
        });
      },
      error: (error: HttpErrorResponse) => {
        const matchConfirmation: MatchConfirmation = this.confirmPossibleDuplicate(error);
        if (matchConfirmation === 'confirmed') {
          this.createDog(this.toCreateRequest(true));
          return;
        }
        this.isSubmitting = false;
        if (matchConfirmation === 'cancelled') {
          this.errorMessage = 'No se guardaron los cambios. Revisa el perro existente y vuelve a intentarlo.';
          return;
        }
        this.handleError(error);
      }
    }));
  }

  private updateDog(request: DogUpdateRequest): void {
    const dogId: number = this.dog?.id ?? 0;
    this.subscriptions.add(this.daycareOperationsService
      .updateDog(dogId, request, this.dogEtag)
      .subscribe({
        next: (result) => {
          this.isSubmitting = false;
          this.router.navigate(['/perros', result.dog.id], {
            queryParams: this.listQueryParams()
          });
        },
        error: (error: HttpErrorResponse) => {
          const matchConfirmation: MatchConfirmation = this.confirmPossibleDuplicate(error);
          if (matchConfirmation === 'confirmed') {
            this.updateDog(this.toUpdateRequest(true));
            return;
          }
          this.isSubmitting = false;
          if (matchConfirmation === 'cancelled') {
            this.errorMessage = 'No se guardaron los cambios. Revisa el perro existente y vuelve a intentarlo.';
            return;
          }
          this.handleError(error);
        }
      }));
  }

  private confirmPossibleDuplicate(error: HttpErrorResponse): MatchConfirmation {
    const message: string = this.getServerMessage(error);
    if (error.status !== 409 || !message.includes('mismo nombre para ese tutor')) {
      return 'not-match';
    }
    return window.confirm(`${message}\n\n¿Quieres continuar igualmente?`)
      ? 'confirmed'
      : 'cancelled';
  }

  private handleError(error: HttpErrorResponse, isTutorSearch: boolean = false): void {
    this.isLoading = false;
    this.isSubmitting = false;
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
    const message: string = error.status === 0
      ? 'No se ha podido conectar con el servidor. Comprueba la conexión e inténtalo de nuevo.'
      : this.getServerMessage(error) || 'No se pudo completar la operación. Revisa los datos e inténtalo de nuevo.';
    if (isTutorSearch) {
      this.tutorErrorMessage = message;
    } else {
      this.errorMessage = message;
    }
  }

  private getServerMessage(error: HttpErrorResponse): string {
    const body: unknown = error.error;
    if (typeof body === 'object' && body !== null && 'message' in body) {
      const message: unknown = (body as { message?: unknown }).message;
      return typeof message === 'string' ? message : '';
    }
    return '';
  }

  private toCreateRequest(confirmMatches: boolean): DogCreateRequest {
    const tutorId: number = this.form.tutorId ?? this.requireTutorId();
    return {
      tutorId,
      name: this.form.name.trim(),
      breed: this.normalizeOptionalText(this.form.breed),
      size: this.requireSize(),
      birthDate: this.form.birthDate || null,
      confirmMatches
    };
  }

  private toUpdateRequest(confirmMatches: boolean): DogUpdateRequest {
    const breed: string | null = this.normalizeOptionalText(this.form.breed);
    const birthDate: string | null = this.form.birthDate || null;
    return {
      name: this.form.name.trim(),
      breed,
      size: this.requireSize(),
      birthDate,
      clearBreed: breed === null,
      clearBirthDate: birthDate === null,
      confirmMatches
    };
  }

  private normalizeOptionalText(value: string): string | null {
    const normalized: string = value.trim();
    return normalized.length === 0 ? null : normalized;
  }

  private requireTutorId(): number {
    if (this.form.tutorId === null) {
      throw new Error('Debes seleccionar un tutor.');
    }
    return this.form.tutorId;
  }

  private requireSize(): DogSize {
    if (this.form.size === null) {
      throw new Error('Debes seleccionar un tamaño.');
    }
    return this.form.size;
  }

  private updateListUrl(): void {
    this.router.navigate(['/perros'], { queryParams: this.listQueryParams() });
  }

  private listQueryParams(): { search: string; page: number } {
    return { search: this.search, page: this.page };
  }

  private emptyForm(): DogForm {
    return {
      tutorId: null,
      name: '',
      breed: '',
      size: null,
      birthDate: ''
    };
  }

  private getToday(): string {
    const now: Date = new Date();
    const month: string = String(now.getMonth() + 1).padStart(2, '0');
    const day: string = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${month}-${day}`;
  }
}
