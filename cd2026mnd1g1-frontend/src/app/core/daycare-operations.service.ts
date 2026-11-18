import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import {
  CheckRecordRequest,
  DailyRecordRequest,
  DailyReservation,
  Dog,
  DogCreateRequest,
  DogPage,
  DogUpdateRequest,
  DogVersionedResponse,
  IncidentRequest,
  ReceptionReservationDetail,
  ReceptionReservationPage,
  Tutor,
  TutorPage,
  TutorRequest,
  TutorVersionedResponse
} from './operational.models';
import { AuthService } from './auth.service';

/**
 * Cliente HTTP de las operaciones diarias de guardería.
 */
@Injectable({ providedIn: 'root' })
export class DaycareOperationsService {
  private readonly reservationsPath: string = '/api/reservas';
  private readonly receptionReservationsPath: string = '/api/recepcion/reservas';
  private readonly tutorsPath: string = '/api/recepcion/tutores';
  private readonly dogsPath: string = '/api/recepcion/perros';

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthService
  ) {}

  getDailyReservations(): Observable<DailyReservation[]> {
    return this.http.get<DailyReservation[]>(this.reservationsPath, {
      headers: this.getAuthHeaders()
    });
  }

  getReceptionReservations(date: string, page: number, size: number): Observable<ReceptionReservationPage> {
    const params: HttpParams = new HttpParams()
      .set('date', date)
      .set('page', page)
      .set('size', size);
    return this.http.get<ReceptionReservationPage>(this.receptionReservationsPath, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getReceptionReservation(reservationId: number): Observable<ReceptionReservationDetail> {
    return this.http.get<ReceptionReservationDetail>(
      `${this.receptionReservationsPath}/${reservationId}`,
      { headers: this.getAuthHeaders() }
    );
  }

  getTutors(search: string, page: number, size: number): Observable<TutorPage> {
    let params: HttpParams = new HttpParams().set('page', page).set('size', size);
    if (search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<TutorPage>(this.tutorsPath, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getTutor(tutorId: number): Observable<TutorVersionedResponse> {
    return this.http.get<Tutor>(`${this.tutorsPath}/${tutorId}`, {
      headers: this.getAuthHeaders(),
      observe: 'response'
    }).pipe(map((response: HttpResponse<Tutor>) => this.mapTutorResponse(response)));
  }

  createTutor(request: TutorRequest): Observable<Tutor> {
    return this.http.post<Tutor>(this.tutorsPath, request, {
      headers: this.getAuthHeaders()
    });
  }

  updateTutor(tutorId: number, request: TutorRequest, etag: string): Observable<TutorVersionedResponse> {
    return this.http.put<Tutor>(`${this.tutorsPath}/${tutorId}`, request, {
      headers: this.getAuthHeaders().set('If-Match', etag),
      observe: 'response'
    }).pipe(map((response: HttpResponse<Tutor>) => this.mapTutorResponse(response)));
  }

  getDogs(search: string, page: number, size: number): Observable<DogPage> {
    let params: HttpParams = new HttpParams().set('page', page).set('size', size);
    if (search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<DogPage>(this.dogsPath, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getDog(dogId: number): Observable<DogVersionedResponse> {
    return this.http.get<Dog>(`${this.dogsPath}/${dogId}`, {
      headers: this.getAuthHeaders(),
      observe: 'response'
    }).pipe(map((response: HttpResponse<Dog>) => this.mapDogResponse(response)));
  }

  createDog(request: DogCreateRequest): Observable<Dog> {
    return this.http.post<Dog>(this.dogsPath, request, {
      headers: this.getAuthHeaders()
    });
  }

  updateDog(dogId: number, request: DogUpdateRequest, etag: string): Observable<DogVersionedResponse> {
    return this.http.put<Dog>(`${this.dogsPath}/${dogId}`, request, {
      headers: this.getAuthHeaders().set('If-Match', etag),
      observe: 'response'
    }).pipe(map((response: HttpResponse<Dog>) => this.mapDogResponse(response)));
  }

  createCheckRecord(reservationId: number, request: CheckRecordRequest): Observable<unknown> {
    return this.http.post<unknown>(
      `${this.reservationsPath}/${reservationId}/check-registros`,
      request,
      { headers: this.getAuthHeaders() }
    );
  }

  private mapTutorResponse(response: HttpResponse<Tutor>): TutorVersionedResponse {
    if (response.body === null) {
      throw new Error('El servidor devolvió una ficha de tutor vacía.');
    }
    const etag: string | null = response.headers.get('ETag');
    if (etag === null) {
      throw new Error('El servidor no devolvió la versión de la ficha.');
    }
    return { tutor: response.body, etag };
  }

  private mapDogResponse(response: HttpResponse<Dog>): DogVersionedResponse {
    if (response.body === null) {
      throw new Error('El servidor devolvió una ficha de perro vacía.');
    }
    const etag: string | null = response.headers.get('ETag');
    if (etag === null) {
      throw new Error('El servidor no devolvió la versión de la ficha del perro.');
    }
    return { dog: response.body, etag };
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
