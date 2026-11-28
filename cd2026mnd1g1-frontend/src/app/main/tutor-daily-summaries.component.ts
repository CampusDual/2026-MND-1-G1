import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';
import { TutorDailySummary, TutorDailySummaryDetail } from '../core/operational.models';
import { NotificationBellComponent } from '../shared/notification-bell.component';

/**
 * Consulta los resúmenes diarios publicados de un perro propio del tutor.
 */
@Component({
  selector: 'app-tutor-daily-summaries',
  standalone: true,
  imports: [CommonModule, RouterLink, NotificationBellComponent],
  templateUrl: './tutor-daily-summaries.component.html',
  styleUrl: './tutor-daily-summaries.component.scss'
})
export class TutorDailySummariesComponent implements OnDestroy {
  @ViewChild(NotificationBellComponent) private notificationBell?: NotificationBellComponent;

  private readonly routeSubscription: Subscription;
  private requestSubscription: Subscription = new Subscription();
  private isDetailRoute: boolean = false;
  dogId: number | null = null;
  summaryId: number | null = null;
  summaries: TutorDailySummary[] = [];
  summary: TutorDailySummaryDetail | null = null;
  loading: boolean = true;
  errorMessage: string = '';
  configurationMessage: string = '';
  notFound: boolean = false;

  constructor(
    private readonly authService: AuthService,
    private readonly operationsService: DaycareOperationsService,
    private readonly router: Router,
    route: ActivatedRoute
  ) {
    this.routeSubscription = route.paramMap.subscribe((parameters) => {
      this.dogId = this.parseIdentifier(parameters.get('dogId'));
      const summaryIdParameter: string | null = parameters.get('summaryId');
      this.isDetailRoute = summaryIdParameter !== null;
      this.summaryId = this.parseIdentifier(summaryIdParameter);
      this.loadCurrentView();
    });
  }

  ngOnDestroy(): void {
    this.routeSubscription.unsubscribe();
    this.requestSubscription.unsubscribe();
  }

  retry(): void {
    this.loadCurrentView();
  }

  returnToSummaries(): void {
    if (this.dogId !== null) {
      void this.router.navigate(['/mis-perros', this.dogId, 'resumenes']);
    } else {
      void this.router.navigate(['/mis-perros']);
    }
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  private loadCurrentView(): void {
    this.requestSubscription.unsubscribe();
    this.requestSubscription = new Subscription();
    this.loading = true;
    this.errorMessage = '';
    this.configurationMessage = '';
    this.notFound = false;
    this.summary = null;
    this.summaries = [];
    if (this.dogId === null || (this.isDetailRoute && this.summaryId === null)) {
      this.notFound = true;
      this.loading = false;
      return;
    }
    if (!this.isDetailRoute) {
      this.loadSummaryList(this.dogId);
      return;
    }
    if (this.summaryId === null) {
      this.notFound = true;
      this.loading = false;
      return;
    }
    this.markViewedAndLoadDetail(this.dogId, this.summaryId);
  }

  private loadSummaryList(dogId: number): void {
    this.requestSubscription.add(this.operationsService.getMyDogDailySummaries(dogId).subscribe({
      next: (summaries) => {
        this.summaries = summaries;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => this.handleError(error)
    }));
  }

  private markViewedAndLoadDetail(dogId: number, summaryId: number): void {
    this.requestSubscription.add(this.operationsService
      .markMyDogDailySummaryAsViewed(dogId, summaryId)
      .subscribe({
        next: () => {
          this.notificationBell?.refresh();
          this.requestSubscription.add(this.operationsService
            .getMyDogDailySummary(dogId, summaryId)
            .subscribe({
              next: (summary) => {
                this.summary = summary;
                this.loading = false;
              },
              error: (error: HttpErrorResponse) => this.handleError(error)
            }));
        },
        error: (error: HttpErrorResponse) => this.handleError(error)
      }));
  }

  private handleError(error: HttpErrorResponse): void {
    this.loading = false;
    if (error.status === 401) {
      this.authService.logout();
      void this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    if (error.status === 404) {
      this.notFound = true;
      return;
    }
    if (this.isTutorAccountNotLinked(error)) {
      this.configurationMessage = 'La cuenta de tutor no está vinculada a una ficha. Contacta con el centro.';
      return;
    }
    this.errorMessage = 'No se pudieron cargar los resúmenes. Inténtalo de nuevo.';
  }

  private isTutorAccountNotLinked(error: HttpErrorResponse): boolean {
    const body: unknown = error.error;
    return typeof body === 'object'
      && body !== null
      && 'code' in body
      && body.code === 'TUTOR_ACCOUNT_NOT_LINKED';
  }

  private parseIdentifier(value: string | null): number | null {
    if (value === null || !/^(0|[1-9]\d*)$/.test(value)) {
      return null;
    }
    const parsed: number = Number(value);
    return Number.isSafeInteger(parsed) ? parsed : null;
  }
}
