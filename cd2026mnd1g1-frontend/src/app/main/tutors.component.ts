import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import { Tutor, TutorRequest, TutorVersionedResponse } from '../core/operational.models';

type TutorScreen = 'list' | 'create' | 'detail' | 'edit';
type MatchConfirmation = 'confirmed' | 'cancelled' | 'not-match';

/**
 * Pantallas de búsqueda, ficha, alta y edición de tutores.
 */
@Component({
  selector: 'app-tutors',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './tutors.component.html',
  styleUrls: ['./tutors.component.scss']
})
export class TutorsComponent implements OnInit, OnDestroy {
  private readonly subscriptions: Subscription = new Subscription();
  private loadSubscription: Subscription | null = null;
  private tutorEtag: string = '';
  private screen: TutorScreen = 'list';

  search: string = '';
  page: number = 0;
  pageSize: number = 50;
  totalPages: number = 0;
  totalElements: number = 0;
  tutors: Tutor[] = [];
  tutor: Tutor | null = null;
  form: TutorRequest = this.emptyForm();
  isLoading: boolean = true;
  isSubmitting: boolean = false;
  errorMessage: string = '';
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
      this.screen = data['screen'] as TutorScreen;
      this.search = parameters.get('search') ?? '';
      this.page = Number(parameters.get('page') ?? '0');
      this.errorMessage = '';
      this.hasAccessError = false;
      this.tutor = null;

      if (this.screen === 'list') {
        this.loadTutors();
      } else if (this.screen === 'create') {
        this.form = this.emptyForm();
        this.isLoading = false;
      } else {
        this.loadTutor();
      }
    }));
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
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

  loadTutors(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.hasAccessError = false;
    this.tutors = [];
    this.loadSubscription?.unsubscribe();
    this.loadSubscription = this.daycareOperationsService
      .getTutors(this.search, this.page, this.pageSize)
      .subscribe({
        next: (result) => {
          this.tutors = result.items;
          this.page = result.page;
          this.totalPages = result.totalPages;
          this.totalElements = result.totalElements;
          this.isLoading = false;
        },
        error: (error: HttpErrorResponse) => this.handleError(error)
      });
  }

  searchTutors(): void {
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

  saveTutor(): void {
    if (this.isSubmitting) {
      return;
    }
    this.isSubmitting = true;
    this.errorMessage = '';
    const request: TutorRequest = this.normalizedForm(false);
    if (this.isCreate) {
      this.createTutor(request);
      return;
    }
    if (this.tutor === null) {
      this.isSubmitting = false;
      this.errorMessage = 'No se ha podido cargar el tutor que quieres editar.';
      return;
    }
    this.updateTutor(request);
  }

  cancelForm(): void {
    this.backToList();
  }

  backToList(): void {
    this.router.navigate(['/tutores'], { queryParams: this.listQueryParams() });
  }

  editTutor(): void {
    if (this.tutor !== null) {
      this.router.navigate(['/tutores', this.tutor.id, 'editar'], {
        queryParams: this.listQueryParams()
      });
    }
  }

  retry(): void {
    if (this.screen === 'list') {
      this.loadTutors();
    } else {
      this.loadTutor();
    }
  }

  reloadTutor(): void {
    if (this.tutor === null || !window.confirm(
      'Al recargar se sustituirán los cambios que aún no has guardado. ¿Quieres continuar?'
    )) {
      return;
    }
    this.loadTutor();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private loadTutor(): void {
    const tutorId: number = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(tutorId) || tutorId < 0) {
      this.isLoading = false;
      this.errorMessage = 'El identificador del tutor no es válido.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.hasAccessError = false;
    this.loadSubscription?.unsubscribe();
    this.loadSubscription = this.daycareOperationsService.getTutor(tutorId).subscribe({
      next: (result: TutorVersionedResponse) => {
        this.tutor = result.tutor;
        this.tutorEtag = result.etag;
        this.form = {
          firstName: result.tutor.firstName,
          lastName: result.tutor.lastName,
          nationalId: result.tutor.nationalId,
          phone: result.tutor.phone,
          email: result.tutor.email,
          confirmMatches: false
        };
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => this.handleError(error)
    });
  }

  private createTutor(request: TutorRequest): void {
    this.subscriptions.add(this.daycareOperationsService.createTutor(request).subscribe({
      next: (created) => {
        this.isSubmitting = false;
        this.router.navigate(['/tutores', created.id], {
          queryParams: this.listQueryParams()
        });
      },
      error: (error: HttpErrorResponse) => {
        const matchConfirmation: MatchConfirmation = this.confirmPossibleMatch(error);
        if (matchConfirmation === 'confirmed') {
          this.createTutor(this.normalizedForm(true));
          return;
        }
        this.isSubmitting = false;
        if (matchConfirmation === 'cancelled') {
          this.errorMessage = 'No se guardaron los cambios. Puedes revisar la coincidencia y volver a intentarlo.';
          return;
        }
        this.handleError(error);
      }
    }));
  }

  private updateTutor(request: TutorRequest): void {
    const tutorId: number = this.tutor?.id ?? 0;
    this.subscriptions.add(this.daycareOperationsService
      .updateTutor(tutorId, request, this.tutorEtag)
      .subscribe({
        next: (result) => {
          this.isSubmitting = false;
          this.router.navigate(['/tutores', result.tutor.id], {
            queryParams: this.listQueryParams()
          });
        },
        error: (error: HttpErrorResponse) => {
          const matchConfirmation: MatchConfirmation = this.confirmPossibleMatch(error);
          if (matchConfirmation === 'confirmed') {
            this.updateTutor(this.normalizedForm(true));
            return;
          }
          this.isSubmitting = false;
          if (matchConfirmation === 'cancelled') {
            this.errorMessage = 'No se guardaron los cambios. Puedes revisar la coincidencia y volver a intentarlo.';
            return;
          }
          this.handleError(error);
        }
      }));
  }

  private confirmPossibleMatch(error: HttpErrorResponse): MatchConfirmation {
    const message: string = this.getServerMessage(error);
    if (error.status !== 409 || !message.includes('mismo teléfono o email')) {
      return 'not-match';
    }
    const confirmed: boolean = window.confirm(`${message}\n\n¿Quieres continuar igualmente?`);
    return confirmed ? 'confirmed' : 'cancelled';
  }

  private handleError(error: HttpErrorResponse): void {
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
    this.errorMessage = error.status === 0
      ? 'No se ha podido conectar con el servidor. Comprueba la conexión e inténtalo de nuevo.'
      : this.getServerMessage(error) || 'No se pudo completar la operación. Revisa los datos e inténtalo de nuevo.';
  }

  private getServerMessage(error: HttpErrorResponse): string {
    const body: unknown = error.error;
    if (typeof body === 'object' && body !== null && 'message' in body) {
      const message: unknown = (body as { message?: unknown }).message;
      return typeof message === 'string' ? message : '';
    }
    return '';
  }

  private normalizedForm(confirmMatches: boolean): TutorRequest {
    return {
      firstName: this.form.firstName.trim(),
      lastName: this.form.lastName.trim(),
      nationalId: this.form.nationalId.trim(),
      phone: this.form.phone.trim(),
      email: this.form.email.trim(),
      confirmMatches
    };
  }

  private updateListUrl(): void {
    this.router.navigate(['/tutores'], { queryParams: this.listQueryParams() });
  }

  private listQueryParams(): { search: string; page: number } {
    return { search: this.search, page: this.page };
  }

  private emptyForm(): TutorRequest {
    return {
      firstName: '',
      lastName: '',
      nationalId: '',
      phone: '',
      email: '',
      confirmMatches: false
    };
  }
}
