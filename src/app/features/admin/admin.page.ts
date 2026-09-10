import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api.service';
import { readApiError } from '../../core/auth/error.interceptor';
import { AccountRole, UserProfile } from '../../core/models';
import { ToastService } from '../../core/ui/toast.service';
import { formatDate } from '../../core/util/format';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

@Component({
  selector: 'app-admin',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, PageHeaderComponent, SpinnerComponent],
  templateUrl: './admin.page.html',
  styleUrl: './admin.page.scss',
})
export class AdminPage {
  private readonly api = inject(AdminApi);
  private readonly toast = inject(ToastService);

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly users = signal<UserProfile[]>([]);
  protected readonly busyId = signal<string | null>(null);
  protected readonly date = formatDate;

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.api
      .users({ size: 100, sort: 'createdAt,desc' })
      .pipe(catchError(() => of(null)))
      .subscribe((page) => {
        if (!page) {
          this.state.set('error');
          return;
        }
        this.users.set(page.content);
        this.state.set('ready');
      });
  }

  private patch(id: string, patch: Partial<UserProfile>): void {
    this.users.update((list) => list.map((u) => (u.id === id ? { ...u, ...patch } : u)));
  }

  protected toggleEnabled(user: UserProfile, enabled: boolean): void {
    this.busyId.set(user.id);
    this.api.updateUser(user.id, { enabled }).subscribe({
      next: (updated) => {
        this.busyId.set(null);
        this.patch(user.id, updated);
        this.toast.success(enabled ? 'Usuario habilitado' : 'Usuario deshabilitado', updated.email);
      },
      error: (err: unknown) => {
        this.busyId.set(null);
        this.toast.error('No se pudo actualizar', readApiError(err)?.message ?? undefined);
      },
    });
  }

  protected changeRole(user: UserProfile, role: AccountRole): void {
    if (role === user.role) return;
    this.busyId.set(user.id);
    this.api.updateUser(user.id, { role }).subscribe({
      next: (updated) => {
        this.busyId.set(null);
        this.patch(user.id, updated);
        this.toast.success('Rol actualizado', `${updated.email} → ${updated.role}`);
      },
      error: (err: unknown) => {
        this.busyId.set(null);
        this.toast.error('No se pudo cambiar el rol', readApiError(err)?.message ?? undefined);
      },
    });
  }
}
