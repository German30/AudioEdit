import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { IconComponent } from '../../shared/icon/icon.component';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent, SpinnerComponent],
  template: `
    <div class="wrap">
      @if (state() === 'working') {
        <app-spinner [size]="26" />
        <h1>Conectando tu cuenta…</h1>
        <p>Estamos validando la respuesta del proveedor.</p>
      } @else {
        <span class="err"><app-icon name="alert" [size]="24" /></span>
        <h1>No pudimos completar el acceso</h1>
        <p>{{ message() }}</p>
        <a routerLink="/login" class="btn btn--ghost">Volver a intentarlo</a>
      }
    </div>
  `,
  styles: [
    `
      :host {
        display: grid;
        place-items: center;
        min-height: 100vh;
        padding: 1.5rem;
      }
      .wrap {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        gap: 0.7rem;
        max-width: 40ch;
      }
      h1 {
        font-size: var(--step-2);
        margin-top: 0.5rem;
      }
      p {
        color: var(--text-dim);
        font-size: var(--step--1);
      }
      .err {
        display: grid;
        place-items: center;
        width: 52px;
        height: 52px;
        border-radius: var(--r-md);
        background: var(--danger-soft);
        color: var(--danger);
      }
      .btn {
        margin-top: 0.75rem;
      }
    `,
  ],
})
export class OauthCallbackPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  protected readonly state = signal<'working' | 'error'>('working');
  protected readonly message = signal('');

  constructor() {
    const q = this.route.snapshot.queryParamMap;
    const error = q.get('error');
    const accessToken = q.get('access_token');
    const refreshToken = q.get('refresh_token');
    const expiresIn = Number(q.get('expires_in') ?? '0');

    if (error) {
      this.fail(decodeURIComponent(error));
      return;
    }
    if (!accessToken || !refreshToken || !expiresIn) {
      this.fail('La URL de retorno no traía los tokens esperados.');
      return;
    }

    this.auth.consumeOAuthTokens(accessToken, refreshToken, expiresIn).subscribe({
      next: () => this.router.navigateByUrl('/app', { replaceUrl: true }),
      error: () => this.fail('El servidor rechazó los tokens recibidos.'),
    });
  }

  private fail(msg: string): void {
    this.state.set('error');
    this.message.set(msg);
  }
}
