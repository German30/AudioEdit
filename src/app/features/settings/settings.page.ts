import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ProfileApi } from '../../core/api/profile-api.service';
import { AuthApi } from '../../core/api/auth-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { fieldErrorText, readApiError } from '../../core/auth/error.interceptor';
import { ToastService } from '../../core/ui/toast.service';
import { formatDate } from '../../core/util/format';
import { IconComponent } from '../../shared/icon/icon.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

@Component({
  selector: 'app-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, PageHeaderComponent, IconComponent, SpinnerComponent],
  templateUrl: './settings.page.html',
  styleUrl: './settings.page.scss',
})
export class SettingsPage {
  private readonly fb = inject(FormBuilder);
  private readonly profileApi = inject(ProfileApi);
  private readonly authApi = inject(AuthApi);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.user;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly loggingOutAll = signal(false);
  protected readonly date = formatDate;

  protected readonly form = this.fb.nonNullable.group({
    displayName: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(120)]],
    avatarUrl: ['', [Validators.maxLength(512)]],
  });

  constructor() {
    effect(() => {
      const u = this.user();
      if (u && this.form.pristine) {
        this.form.patchValue({ displayName: u.displayName, avatarUrl: u.avatarUrl ?? '' }, { emitEvent: false });
      }
    });
  }

  protected providerLabel(): string {
    const p = this.user()?.provider;
    return p === 'GOOGLE' ? 'Google' : p === 'GITHUB' ? 'GitHub' : 'Acceso local (dev)';
  }

  protected save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const { displayName, avatarUrl } = this.form.getRawValue();

    this.profileApi.update({ displayName: displayName.trim(), avatarUrl: avatarUrl.trim() || undefined }).subscribe({
      next: () => {
        this.auth.refreshProfile().subscribe();
        this.saving.set(false);
        this.form.markAsPristine();
        this.toast.success('Perfil actualizado');
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.error.set(fieldErrorText(err) ?? readApiError(err)?.message ?? 'No se pudo guardar.');
      },
    });
  }

  protected logoutEverywhere(): void {
    if (!confirm('Se cerrará la sesión en todos tus dispositivos. ¿Continuar?')) return;
    this.loggingOutAll.set(true);
    this.authApi.logoutAll().subscribe({
      next: () => {
        this.auth.logout().subscribe(() => this.router.navigateByUrl('/login'));
      },
      error: () => {
        this.loggingOutAll.set(false);
        this.toast.error('No se pudo cerrar todas las sesiones');
      },
    });
  }
}
