import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';

export interface AuthenticatedUser {
  id: number;
  username: string;
  enabled: boolean;
  roles: string[];
}

/**
 * Servicio de autenticación mínimo con Basic Auth contra el backend.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authenticationPath: string = '/api/users/me';
  private readonly sessionHeaderKey: string = 'pawcare.auth.header';
  private readonly sessionUserKey: string = 'pawcare.auth.user';
  private readonly authenticatedSignal = signal<boolean>(false);
  private readonly authHeaderSignal = signal<string>('');
  private readonly errorMessageSignal = signal<string>('');
  private readonly currentUserSignal = signal<AuthenticatedUser | null>(null);

  constructor(private readonly http: HttpClient) {
    this.restoreSession();
  }

  isAuthenticated(): boolean {
    return this.authenticatedSignal();
  }

  getCurrentUser(): AuthenticatedUser | null {
    return this.currentUserSignal();
  }

  hasRole(role: string): boolean {
    const expectedRole: string = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
    return this.currentUserSignal()?.roles.some(
      (userRole: string) => userRole === expectedRole
    ) ?? false;
  }

  getErrorMessage(): string {
    return this.errorMessageSignal();
  }

  login(username: string, password: string): Observable<boolean> {
    const normalizedUsername: string = username?.trim() ?? '';
    const normalizedPassword: string = password ?? '';

    if (!normalizedUsername || !normalizedPassword) {
      this.logout();
      return of(false);
    }

    this.logout();
    const token: string = btoa(unescape(encodeURIComponent(`${normalizedUsername}:${normalizedPassword}`)));
    const authHeader: string = `Basic ${token}`;
    const headers: HttpHeaders = new HttpHeaders({ Authorization: authHeader });

    this.errorMessageSignal.set('');

    return this.http.get<AuthenticatedUser>(this.authenticationPath, { headers }).pipe(
      map((user: AuthenticatedUser) => {
        try {
          sessionStorage.setItem(this.sessionHeaderKey, authHeader);
          sessionStorage.setItem(this.sessionUserKey, JSON.stringify(user));
          this.authHeaderSignal.set(authHeader);
          this.currentUserSignal.set(user);
          this.authenticatedSignal.set(true);
          return true;
        } catch {
          this.logout();
          this.errorMessageSignal.set('No se pudo mantener la sesión en esta pestaña.');
          return false;
        }
      }),
      catchError((error: HttpErrorResponse) => {
        this.logout();
        if (error.status === 401 || error.status === 403) {
          this.errorMessageSignal.set('Credenciales incorrectas.');
        } else if (error.status === 0) {
          this.errorMessageSignal.set('No se puede conectar con el backend en localhost:8080.');
        } else {
          this.errorMessageSignal.set('Error inesperado durante la autenticación.');
        }
        return of(false);
      })
    );
  }

  logout(): void {
    this.authHeaderSignal.set('');
    this.authenticatedSignal.set(false);
    this.currentUserSignal.set(null);
    this.errorMessageSignal.set('');
    try {
      sessionStorage.removeItem(this.sessionHeaderKey);
      sessionStorage.removeItem(this.sessionUserKey);
    } catch {
      this.errorMessageSignal.set('No se pudo limpiar completamente la sesión del navegador.');
    }
  }

  getAuthHeader(): string {
    return this.authHeaderSignal();
  }

  private restoreSession(): void {
    try {
      const authHeader: string | null = sessionStorage.getItem(this.sessionHeaderKey);
      const serializedUser: string | null = sessionStorage.getItem(this.sessionUserKey);
      if (authHeader === null && serializedUser === null) {
        return;
      }
      if (authHeader === null || serializedUser === null) {
        this.logout();
        return;
      }
      const user: unknown = JSON.parse(serializedUser);
      if (!this.isAuthenticatedUser(user) || !authHeader.startsWith('Basic ')) {
        this.logout();
        return;
      }
      this.authHeaderSignal.set(authHeader);
      this.currentUserSignal.set(user);
      this.authenticatedSignal.set(true);
    } catch {
      this.logout();
      this.errorMessageSignal.set('No se pudo recuperar la sesión del navegador. Inicia sesión de nuevo.');
    }
  }

  private isAuthenticatedUser(value: unknown): value is AuthenticatedUser {
    if (typeof value !== 'object' || value === null) {
      return false;
    }
    const user: Record<string, unknown> = value as Record<string, unknown>;
    return typeof user['id'] === 'number'
      && typeof user['username'] === 'string'
      && typeof user['enabled'] === 'boolean'
      && Array.isArray(user['roles'])
      && user['roles'].every((role: unknown) => typeof role === 'string');
  }
}