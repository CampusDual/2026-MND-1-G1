import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { roleGuard } from './core/role.guard';
import { LoginComponent } from './auth/login.component';
import { ItWorksComponent } from './main/it-works.component';
import { ReservationDetailComponent } from './main/reservation-detail.component';
import { TutorsComponent } from './main/tutors.component';
import { DogsComponent } from './main/dogs.component';
import { ReservationCreateComponent } from './main/reservation-create.component';
import { ReceptionArrivalComponent } from './main/reception-arrival.component';
import { ReceptionDepartureComponent } from './main/reception-departure.component';
import { TutorDogsComponent } from './main/tutor-dogs.component';
import { CaregiverDogsComponent } from './main/caregiver-dogs.component';
import { GroomingRequestsComponent } from './main/grooming-requests.component';
import { GroomingRequestDetailComponent } from './main/grooming-request-detail.component';
import { ReceptionGroomingRequestsComponent } from './main/reception-grooming-requests.component';
import { ReceptionGroomingRequestDetailComponent } from './main/reception-grooming-request-detail.component';
import { GroomingAppointmentCreateComponent } from './main/grooming-appointment-create.component';
import { GroomerAgendaComponent } from './main/groomer-agenda.component';
import { TutorDailySummariesComponent } from './main/tutor-daily-summaries.component';

const receptionRoles: string[] = ['RECEPCIONISTA', 'ADMIN', 'GERENTE'];

export const routes: Routes = [
	{ path: 'login', component: LoginComponent },
	{ path: 'mis-perros-hoy', component: CaregiverDogsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['CUIDADOR'] } },
	{ path: 'mis-perros-hoy/:reservationId', component: CaregiverDogsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['CUIDADOR'] } },
	{ path: 'mis-perros/nuevo', component: TutorDogsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TUTOR'] } },
	{ path: 'mis-perros/:id/editar', component: TutorDogsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TUTOR'] } },
	{ path: 'mis-perros/:dogId/resumenes/:summaryId', component: TutorDailySummariesComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TUTOR'] } },
	{ path: 'mis-perros/:dogId/resumenes', component: TutorDailySummariesComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TUTOR'] } },
	{ path: 'mis-perros', component: TutorDogsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TUTOR'] } },
	{ path: 'mis-perros/:id', component: TutorDogsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TUTOR'] } },
	{ path: 'tablero-diario/reservas/:id/entrada', component: ReceptionArrivalComponent, canActivate: [authGuard, roleGuard], data: { roles: receptionRoles } },
	{ path: 'tablero-diario/reservas/:id/salida', component: ReceptionDepartureComponent, canActivate: [authGuard, roleGuard], data: { roles: receptionRoles } },
	{ path: 'solicitudes-peluqueria/:id', component: ReceptionGroomingRequestDetailComponent, canActivate: [authGuard, roleGuard], data: { roles: receptionRoles } },
	{ path: 'solicitudes-peluqueria', component: ReceptionGroomingRequestsComponent, canActivate: [authGuard, roleGuard], data: { roles: receptionRoles } },
	{ path: 'citas-peluqueria/nueva', component: GroomingAppointmentCreateComponent, canActivate: [authGuard, roleGuard], data: { roles: ['RECEPCIONISTA'] } },
	{ path: 'agenda-peluqueros', component: GroomerAgendaComponent, canActivate: [authGuard, roleGuard], data: { roles: ['RECEPCIONISTA'] } },
	{ path: 'citas-peluqueria/:id', component: GroomingRequestDetailComponent, canActivate: [authGuard, roleGuard], data: { roles: receptionRoles } },
	{ path: 'citas-peluqueria', component: GroomingRequestsComponent, canActivate: [authGuard, roleGuard], data: { roles: receptionRoles } },
	{ path: 'tablero-diario/reservas/:id', component: ReservationDetailComponent, canActivate: [authGuard, roleGuard], data: { roles: receptionRoles } },
	{ path: 'reservas/nueva', component: ReservationCreateComponent, canActivate: [authGuard, roleGuard], data: { roles: receptionRoles } },
	{ path: 'tablero-diario', component: ItWorksComponent, canActivate: [authGuard, roleGuard], data: { roles: receptionRoles } },
	{ path: 'tutores', component: TutorsComponent, canActivate: [authGuard, roleGuard], data: { screen: 'list', roles: receptionRoles } },
	{ path: 'tutores/nuevo', component: TutorsComponent, canActivate: [authGuard, roleGuard], data: { screen: 'create', roles: receptionRoles } },
	{ path: 'tutores/:id/editar', component: TutorsComponent, canActivate: [authGuard, roleGuard], data: { screen: 'edit', roles: receptionRoles } },
	{ path: 'tutores/:id', component: TutorsComponent, canActivate: [authGuard, roleGuard], data: { screen: 'detail', roles: receptionRoles } },
	{ path: 'perros', component: DogsComponent, canActivate: [authGuard, roleGuard], data: { screen: 'list', roles: receptionRoles } },
	{ path: 'perros/nuevo', component: DogsComponent, canActivate: [authGuard, roleGuard], data: { screen: 'create', roles: receptionRoles } },
	{ path: 'perros/:id/editar', component: DogsComponent, canActivate: [authGuard, roleGuard], data: { screen: 'edit', roles: receptionRoles } },
	{ path: 'perros/:id', component: DogsComponent, canActivate: [authGuard, roleGuard], data: { screen: 'detail', roles: receptionRoles } },
	{ path: '', pathMatch: 'full', redirectTo: 'login' },
	{ path: '**', redirectTo: 'login' }
];
