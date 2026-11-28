import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import {
  CheckRecordRequest,
  DailyReservation,
  Dog,
  DogCreateRequest,
  DogDetail,
  DogDetailVersionedResponse,
  DogPage,
  DogUpdateRequest,
  DogVersionedResponse,
  IncidentRequest,
  ReceptionAvailability,
  ReceptionArrivalRequest,
  ReceptionArrivalResult,
  ReceptionDepartureData,
  ReceptionDepartureRequest,
  ReceptionDepartureResult,
  ReceptionGroupOption,
  ReceptionReservationCreateRequest,
  ReceptionReservationCreated,
  ReceptionReservationDetail,
  ReceptionReservationPage,
  CaregiverDogPage,
  CaregiverDogDetail,
  DailyRecord,
  DailyRecordRequest,
  CaregiverDailySummary,
  GroomingAppointment,
  GroomingAppointmentStatus,
  GroomingAppointmentDetail,
  CreateGroomingAppointmentRequest,
  GroomingDogOptionPage,
  GroomingServiceOption,
  GroomingRequestOrigin,
  GroomingAppointmentPage,
  GroomingRequest,
  GroomingRequestDetail,
  GroomingRequestPage,
  GroomingRequestStatus,
  Incident,
  IncidentSeverity,
  Tutor,
  TutorPage,
  TutorRequest,
  TutorVersionedResponse,
  TutorDogPage,
  TutorDogDetail,
  TutorDogVersionedResponse,
  TutorAuthorizedPerson,
  TutorDogCreateRequest,
  TutorDogUpdateRequest
} from './operational.models';
import { AuthService } from './auth.service';

/**
 * Cliente HTTP de las operaciones diarias de guardería.
 */
@Injectable({ providedIn: 'root' })
export class DaycareOperationsService {
  private readonly reservationsPath: string = '/api/reservas';
  private readonly receptionReservationsPath: string = '/api/recepcion/reservas';
  private readonly groomingAppointmentsPath: string = '/api/recepcion/citas-peluqueria';
  private readonly groomingRequestsPath: string = '/api/recepcion/solicitudes-peluqueria';
  private readonly receptionPath: string = '/api/recepcion';
  private readonly tutorsPath: string = '/api/recepcion/tutores';
  private readonly dogsPath: string = '/api/recepcion/perros';
  private readonly tutorDogsPath: string = '/api/tutor/mis-perros';
  private readonly caregiverDogsPath: string = '/api/cuidador/mis-perros-hoy';

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthService
  ) {}

  getDailyReservations(): Observable<DailyReservation[]> {
    return this.http.get<DailyReservation[]>(this.reservationsPath, {
      headers: this.getAuthHeaders()
    });
  }

  getMyDogs(page: number, size: number): Observable<TutorDogPage> {
    const params: HttpParams = new HttpParams().set('page', page).set('size', size);
    return this.http.get<TutorDogPage>(this.tutorDogsPath, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getCaregiverTodayDogs(page: number, size: number): Observable<CaregiverDogPage> {
    const params: HttpParams = new HttpParams().set('page', page).set('size', size);
    return this.http.get<CaregiverDogPage>(this.caregiverDogsPath, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getCaregiverTodayDog(reservationId: number): Observable<CaregiverDogDetail> {
    return this.http.get<CaregiverDogDetail>(`${this.caregiverDogsPath}/${reservationId}`, {
      headers: this.getAuthHeaders()
    });
  }

  getCaregiverTodayDogPhoto(reservationId: number): Observable<Blob> {
    return this.http.get(`${this.caregiverDogsPath}/${reservationId}/foto`, {
      headers: this.getAuthHeaders(),
      responseType: 'blob'
    });
  }

  getCaregiverDailyRecords(reservationId: number): Observable<DailyRecord[]> {
    return this.http.get<DailyRecord[]>(
      `${this.caregiverDogsPath}/${reservationId}/registros-diarios`,
      { headers: this.getAuthHeaders() }
    );
  }

  createCaregiverDailyRecord(
    reservationId: number,
    request: DailyRecordRequest
  ): Observable<DailyRecord> {
    return this.http.post<DailyRecord>(
      `${this.caregiverDogsPath}/${reservationId}/registros-diarios`,
      request,
      { headers: this.getAuthHeaders() }
    );
  }

  getCaregiverDailySummary(reservationId: number): Observable<CaregiverDailySummary | null> {
    return this.http.get<CaregiverDailySummary | null>(
      `${this.caregiverDogsPath}/${reservationId}/resumen`,
      { headers: this.getAuthHeaders() }
    );
  }

  publishCaregiverDailySummary(
    reservationId: number,
    text: string
  ): Observable<CaregiverDailySummary> {
    return this.http.post<CaregiverDailySummary>(
      `${this.caregiverDogsPath}/${reservationId}/resumen`,
      { text },
      { headers: this.getAuthHeaders() }
    );
  }

  getCaregiverIncidents(reservationId: number): Observable<Incident[]> {
    return this.http.get<Incident[]>(
      `${this.caregiverDogsPath}/${reservationId}/incidencias`,
      { headers: this.getAuthHeaders() }
    );
  }

  createCaregiverIncident(
    reservationId: number,
    severity: IncidentSeverity,
    description: string
  ): Observable<Incident> {
    return this.http.post<Incident>(
      `${this.caregiverDogsPath}/${reservationId}/incidencias`,
      { severity, description },
      { headers: this.getAuthHeaders() }
    );
  }

  getMyDog(dogId: number): Observable<TutorDogVersionedResponse> {
    return this.http.get<TutorDogDetail>(`${this.tutorDogsPath}/${dogId}`, {
      headers: this.getAuthHeaders(),
      observe: 'response'
    }).pipe(map((response: HttpResponse<TutorDogDetail>) => this.mapTutorDogResponse(response)));
  }

  getMyDogPhoto(dogId: number): Observable<Blob> {
    return this.http.get(`${this.tutorDogsPath}/${dogId}/foto`, {
      headers: this.getAuthHeaders(),
      responseType: 'blob'
    });
  }

  getMyAuthorizedPersons(): Observable<TutorAuthorizedPerson[]> {
    return this.http.get<TutorAuthorizedPerson[]>(`${this.tutorDogsPath}/personas-autorizadas`, {
      headers: this.getAuthHeaders()
    });
  }

  createMyDog(
    request: TutorDogCreateRequest,
    photo: File | null,
    idempotencyKey: string
  ): Observable<TutorDogDetail> {
    const formData: FormData = new FormData();
    formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    if (photo !== null) {
      formData.append('photo', photo, photo.name);
    }
    return this.http.post<TutorDogDetail>(this.tutorDogsPath, formData, {
      headers: this.getAuthHeaders().set('Idempotency-Key', idempotencyKey)
    });
  }

  updateMyDog(
    dogId: number,
    request: TutorDogUpdateRequest,
    photo: File | null,
    etag: string
  ): Observable<TutorDogVersionedResponse> {
    const formData: FormData = new FormData();
    formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    if (photo !== null) {
      formData.append('photo', photo, photo.name);
    }
    return this.http.put<TutorDogDetail>(`${this.tutorDogsPath}/${dogId}`, formData, {
      headers: this.getAuthHeaders().set('If-Match', etag),
      observe: 'response'
    }).pipe(map((response: HttpResponse<TutorDogDetail>) => this.mapTutorDogResponse(response)));
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

  getReceptionGroomingAppointments(
    filters: {
      origin: GroomingRequestOrigin | '';
      status: GroomingAppointmentStatus | '';
      from: string;
      to: string;
    },
    page: number,
    size: number
  ): Observable<GroomingAppointmentPage> {
    let params: HttpParams = new HttpParams().set('page', page).set('size', size);
    if (filters.origin) {
      params = params.set('origin', filters.origin);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    return this.http.get<GroomingAppointmentPage>(this.groomingAppointmentsPath, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getReceptionGroomingAppointment(appointmentId: number): Observable<GroomingAppointmentDetail> {
    return this.http.get<GroomingAppointmentDetail>(`${this.groomingAppointmentsPath}/${appointmentId}`, {
      headers: this.getAuthHeaders()
    });
  }

  getReceptionGroomingDogs(search: string, page: number, size: number): Observable<GroomingDogOptionPage> {
    let params: HttpParams = new HttpParams().set('page', page).set('size', size);
    if (search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<GroomingDogOptionPage>(`${this.groomingAppointmentsPath}/perros`, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getReceptionGroomingServices(): Observable<GroomingServiceOption[]> {
    return this.http.get<GroomingServiceOption[]>(`${this.groomingAppointmentsPath}/servicios`, {
      headers: this.getAuthHeaders()
    });
  }

  createReceptionGroomingAppointment(
    request: CreateGroomingAppointmentRequest
  ): Observable<GroomingAppointmentDetail> {
    return this.http.post<GroomingAppointmentDetail>(this.groomingAppointmentsPath, request, {
      headers: this.getAuthHeaders()
    });
  }

  getReceptionGroomingRequests(
    filters: {
      origin: GroomingRequestOrigin | '';
      status: GroomingRequestStatus | '';
      from: string;
      to: string;
    },
    page: number,
    size: number
  ): Observable<GroomingRequestPage> {
    let params: HttpParams = new HttpParams().set('page', page).set('size', size);
    if (filters.origin) {
      params = params.set('origin', filters.origin);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    return this.http.get<GroomingRequestPage>(this.groomingRequestsPath, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getReceptionGroomingRequest(requestId: number): Observable<GroomingRequestDetail> {
    return this.http.get<GroomingRequestDetail>(`${this.groomingRequestsPath}/${requestId}`, {
      headers: this.getAuthHeaders()
    });
  }

  confirmReceptionGroomingRequest(requestId: number): Observable<GroomingRequestDetail> {
    return this.http.post<GroomingRequestDetail>(
      `${this.groomingRequestsPath}/${requestId}/confirmar`,
      null,
      { headers: this.getAuthHeaders() }
    );
  }

  rejectReceptionGroomingRequest(requestId: number): Observable<GroomingRequestDetail> {
    return this.http.post<GroomingRequestDetail>(
      `${this.groomingRequestsPath}/${requestId}/rechazar`,
      null,
      { headers: this.getAuthHeaders() }
    );
  }

  registerReceptionArrival(
    reservationId: number,
    request: ReceptionArrivalRequest
  ): Observable<ReceptionArrivalResult> {
    return this.http.post<ReceptionArrivalResult>(
      `${this.receptionReservationsPath}/${reservationId}/entrada`,
      request,
      { headers: this.getAuthHeaders() }
    );
  }

  getReceptionDepartureData(reservationId: number): Observable<ReceptionDepartureData> {
    return this.http.get<ReceptionDepartureData>(
      `${this.receptionReservationsPath}/${reservationId}/datos-salida`,
      { headers: this.getAuthHeaders() }
    );
  }

  registerReceptionDeparture(
    reservationId: number,
    request: ReceptionDepartureRequest
  ): Observable<ReceptionDepartureResult> {
    return this.http.post<ReceptionDepartureResult>(
      `${this.receptionReservationsPath}/${reservationId}/salida`,
      request,
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

  getReservationDogs(search: string, page: number, size: number): Observable<DogPage> {
    let params: HttpParams = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('reservationCandidates', true);
    if (search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<DogPage>(this.dogsPath, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getReceptionGroups(): Observable<ReceptionGroupOption[]> {
    return this.http.get<ReceptionGroupOption[]>(`${this.receptionPath}/grupos`, {
      headers: this.getAuthHeaders()
    });
  }

  getReceptionAvailability(date: string, groupId: number): Observable<ReceptionAvailability> {
    const params: HttpParams = new HttpParams()
      .set('date', date)
      .set('groupId', groupId);
    return this.http.get<ReceptionAvailability>(`${this.receptionPath}/disponibilidad`, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  createReceptionReservation(
    request: ReceptionReservationCreateRequest
  ): Observable<ReceptionReservationCreated> {
    return this.http.post<ReceptionReservationCreated>(
      this.receptionReservationsPath,
      request,
      { headers: this.getAuthHeaders() }
    );
  }

  getDog(dogId: number): Observable<DogDetailVersionedResponse> {
    return this.http.get<DogDetail>(`${this.dogsPath}/${dogId}`, {
      headers: this.getAuthHeaders(),
      observe: 'response'
    }).pipe(map((response: HttpResponse<DogDetail>) => this.mapDogDetailResponse(response)));
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

  private mapTutorDogResponse(response: HttpResponse<TutorDogDetail>): TutorDogVersionedResponse {
    if (response.body === null) {
      throw new Error('El servidor devolvió una ficha de perro vacía.');
    }
    const etag: string | null = response.headers.get('ETag');
    if (etag === null) {
      throw new Error('El servidor no devolvió la versión de la ficha del perro.');
    }
    return { dog: response.body, etag };
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

  private mapDogDetailResponse(response: HttpResponse<DogDetail>): DogDetailVersionedResponse {
    if (response.body === null) {
      throw new Error('El servidor devolvió una ficha de perro vacía.');
    }
    const etag: string | null = response.headers.get('ETag');
    if (etag === null) {
      throw new Error('El servidor no devolvió la versión de la ficha del perro.');
    }
    return {
      dog: response.body.dog,
      authorizedPersons: response.body.authorizedPersons,
      etag
    };
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
