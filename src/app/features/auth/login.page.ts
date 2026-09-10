import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AuthApi } from '../../core/api/auth-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { fieldErrorText, readApiError } from '../../core/auth/error.interceptor';
import { ProviderInfo } from '../../core/models';
import { IconComponent } from '../../shared/icon/icon.component';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';
import { WaveformComponent } from '../../shared/waveform/waveform.component';

@Component({
  selector: 'app-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, IconComponent, SpinnerComponent, WaveformComponent],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly authApi = inject(AuthApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    displayName: [''],
  });

  protected readonly providers = signal<ProviderInfo[]>([]);
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.authApi
      .providers()
      .pipe(catchError(() => of([] as ProviderInfo[])))
      .subscribe((list) => this.providers.set(list));
  }

  protected providerLabel(id: string): string {
    return id === 'google' ? 'Google' : id === 'github' ? 'GitHub' : id;
  }

  protected startOAuth(provider: ProviderInfo): void {
    window.location.href = this.authApi.authorizeUrl(provider);
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const { email, displayName } = this.form.getRawValue();

    this.auth.devLogin(email, displayName.trim() || undefined).subscribe({
      next: () => {
        const next = this.route.snapshot.queryParamMap.get('next');
        this.router.navigateByUrl(next && next.startsWith('/') ? next : '/app');
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(
          fieldErrorText(err) ??
            readApiError(err)?.message ??
            'No se pudo iniciar sesión. ¿La API está en marcha con el perfil dev?',
        );
      },
    });
  }
}
