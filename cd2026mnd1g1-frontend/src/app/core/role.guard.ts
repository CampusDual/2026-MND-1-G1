import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Limita el acceso visual a espacios según el rol ya devuelto por la identidad autenticada.
 */
export const roleGuard: CanActivateFn = (route) => {
  const authService: AuthService = inject(AuthService);
  const router: Router = inject(Router);
  const allowedRoles: string[] = route.data['roles'] as string[] ?? [];

  if (allowedRoles.some((role: string) => authService.hasRole(role))) {
    return true;
  }
  if (authService.hasRole('TUTOR')) {
    return router.createUrlTree(['/mis-perros']);
  }
  if (authService.hasRole('CUIDADOR')) {
    return router.createUrlTree(['/mis-perros-hoy']);
  }
  if (authService.hasRole('RECEPCIONISTA')
      || authService.hasRole('ADMIN')
      || authService.hasRole('GERENTE')) {
    return router.createUrlTree(['/tablero-diario']);
  }
  return router.createUrlTree(['/login']);
};
