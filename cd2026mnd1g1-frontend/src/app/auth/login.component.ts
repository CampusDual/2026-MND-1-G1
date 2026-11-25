import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

/**
 * Pantalla de acceso mínima basada en Basic Auth.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  isLoading: boolean = false;
  errorMessage: string = '';
  showPassword: boolean = false;

  readonly loginForm;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {
    this.loginForm = this.formBuilder.nonNullable.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  onSubmit(): void {
    this.errorMessage = '';

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const { username, password } = this.loginForm.getRawValue();
    this.isLoading = true;

    this.authService.login(username, password).subscribe({
      next: (isAuthenticated: boolean) => {
        if (isAuthenticated) {
          const destination: string = this.authService.hasRole('CUIDADOR')
            ? '/mis-perros-hoy'
            : this.authService.hasRole('TUTOR')
              ? '/mis-perros'
              : this.getSafeReturnUrl();
          this.router.navigateByUrl(destination);
          return;
        }

        this.errorMessage = this.authService.getErrorMessage() || 'Credenciales incorrectas o backend no disponible.';
      },
      error: () => {
        this.errorMessage = 'Se ha producido un error durante el login.';
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  private getSafeReturnUrl(): string {
    const returnUrl: string | null = this.route.snapshot.queryParamMap.get('returnUrl');
    if (returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//') && !returnUrl.startsWith('/login')) {
      return returnUrl;
    }
    return '/tablero-diario';
  }
}
