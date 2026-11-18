import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './auth/login.component';
import { ItWorksComponent } from './main/it-works.component';
import { ReservationDetailComponent } from './main/reservation-detail.component';
import { TutorsComponent } from './main/tutors.component';
import { DogsComponent } from './main/dogs.component';
import { ReservationCreateComponent } from './main/reservation-create.component';

export const routes: Routes = [
	{ path: 'login', component: LoginComponent },
	{ path: 'tablero-diario/reservas/:id', component: ReservationDetailComponent, canActivate: [authGuard] },
	{ path: 'reservas/nueva', component: ReservationCreateComponent, canActivate: [authGuard] },
	{ path: 'tablero-diario', component: ItWorksComponent, canActivate: [authGuard] },
	{ path: 'tutores', component: TutorsComponent, canActivate: [authGuard], data: { screen: 'list' } },
	{ path: 'tutores/nuevo', component: TutorsComponent, canActivate: [authGuard], data: { screen: 'create' } },
	{ path: 'tutores/:id/editar', component: TutorsComponent, canActivate: [authGuard], data: { screen: 'edit' } },
	{ path: 'tutores/:id', component: TutorsComponent, canActivate: [authGuard], data: { screen: 'detail' } },
	{ path: 'perros', component: DogsComponent, canActivate: [authGuard], data: { screen: 'list' } },
	{ path: 'perros/nuevo', component: DogsComponent, canActivate: [authGuard], data: { screen: 'create' } },
	{ path: 'perros/:id/editar', component: DogsComponent, canActivate: [authGuard], data: { screen: 'edit' } },
	{ path: 'perros/:id', component: DogsComponent, canActivate: [authGuard], data: { screen: 'detail' } },
	{ path: '', pathMatch: 'full', redirectTo: 'login' },
	{ path: '**', redirectTo: 'login' }
];
