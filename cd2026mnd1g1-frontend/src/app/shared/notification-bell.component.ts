import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { DaycareOperationsService } from '../core/daycare-operations.service';

/**
 * Muestra el contador interno de novedades sin desplegar un listado.
 */
@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (errorMessage) {
      <button class="notification-bell error" type="button" (click)="refresh()" [attr.title]="errorMessage" aria-label="Error al cargar novedades. Reintentar">
        <span aria-hidden="true">🔔</span><span>!</span>
      </button>
    } @else {
      <span class="notification-bell" role="status" [attr.aria-label]="ariaLabel">
        <span aria-hidden="true">🔔</span>
        @if (!loading && unreadCount > 0) {
          <span class="notification-badge">{{ unreadCount }}</span>
        }
      </span>
    }
  `,
  styles: [`
    .notification-bell {
      position: relative;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 2.5rem;
      min-height: 2.5rem;
      color: #233044;
      font-size: 1.35rem;
    }

    .notification-bell.error {
      border: 0;
      border-radius: 50%;
      background: transparent;
      color: #a12622;
      cursor: pointer;
    }

    .notification-badge {
      position: absolute;
      top: 0;
      right: 0;
      min-width: 1.2rem;
      padding: 0.1rem 0.25rem;
      border-radius: 1rem;
      background: #b42318;
      color: #fff;
      font-size: 0.7rem;
      font-weight: 700;
      line-height: 1.2;
      text-align: center;
    }
  `]
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  private requestSubscription: Subscription | null = null;
  unreadCount: number = 0;
  loading: boolean = true;
  errorMessage: string = '';

  constructor(
    private readonly operationsService: DaycareOperationsService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.refresh();
  }

  ngOnDestroy(): void {
    this.requestSubscription?.unsubscribe();
  }

  get ariaLabel(): string {
    return this.unreadCount === 1
      ? '1 novedad sin leer'
      : `${this.unreadCount} novedades sin leer`;
  }

  refresh(): void {
    this.requestSubscription?.unsubscribe();
    this.loading = true;
    this.errorMessage = '';
    this.requestSubscription = this.operationsService.getMyNotificationCount().subscribe({
      next: (result) => {
        if (!Number.isSafeInteger(result.unreadCount) || result.unreadCount < 0) {
          this.errorMessage = 'El contador de novedades recibido no es válido.';
        } else {
          this.unreadCount = result.unreadCount;
        }
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        if (error.status === 401) {
          this.authService.logout();
          void this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
          return;
        }
        this.errorMessage = this.isTutorAccountNotLinked(error)
          ? 'La cuenta de tutor no está vinculada a una ficha.'
          : 'No se pudieron cargar las novedades.';
      }
    });
  }

  private isTutorAccountNotLinked(error: HttpErrorResponse): boolean {
    const body: unknown = error.error;
    return typeof body === 'object'
      && body !== null
      && 'code' in body
      && body.code === 'TUTOR_ACCOUNT_NOT_LINKED';
  }
}
