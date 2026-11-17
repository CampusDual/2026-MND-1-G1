import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CheckRecordRequest,
  DailyRecordRequest,
  DailyReservation,
  IncidentRequest
} from './operational.models';
import { AuthService } from './auth.service';

/**
 * Cliente HTTP de las operaciones diarias de guardería.
 */
@Injectable({ providedIn: 'root' })
export class DaycareOperationsService {
  private readonly reservationsPath: string = '/api/reservas';

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthService
  ) {}

  getDailyReservations(): Observable<DailyReservation[]> {
    return this.http.get<DailyReservation[]>(this.reservationsPath, {
      headers: this.getAuthHeaders()
    });
  }

  createCheckRecord(reservationId: number, request: CheckRecordRequest): Observable<unknown> {
    return this.http.post<unknown>(
      `${this.reservationsPath}/${reservationId}/check-registros`,
      request,
      { headers: this.getAuthHeaders() }
    );
  }

  createDailyRecord(reservationId: number, request: DailyRecordRequest): Observable<unknown> {
    return this.http.post<unknown>(
      `${this.reservationsPath}/${reservationId}/registros-diarios`,
      request,
      { headers: this.getAuthHeaders() }
    );
  }

  createIncident(reservationId: number, request: IncidentRequest): Observable<unknown> {
    return this.http.post<unknown>(
      `${this.reservationsPath}/${reservationId}/incidencias`,
      request,
      { headers: this.getAuthHeaders() }
    );
  }

  private getAuthHeaders(): HttpHeaders {
    const authHeader: string = this.authService.getAuthHeader();
    if (!authHeader) {
      throw new Error('No hay credenciales de sesión disponibles.');
    }
    return new HttpHeaders({ Authorization: authHeader });
  }
}
