import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './auth/login.component';
import { ItWorksComponent } from './main/it-works.component';
import { ReservationDetailComponent } from './main/reservation-detail.component';

export const routes: Routes = [
	{ path: 'login', component: LoginComponent },
	{ path: 'tablero-diario/reservas/:id', component: ReservationDetailComponent, canActivate: [authGuard] },
	{ path: 'tablero-diario', component: ItWorksComponent, canActivate: [authGuard] },
	{ path: '', pathMatch: 'full', redirectTo: 'login' },
	{ path: '**', redirectTo: 'login' }
];
