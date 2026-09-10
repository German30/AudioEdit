import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ProjectsApi } from '../../core/api/projects-api.service';
import { fieldErrorText, readApiError } from '../../core/auth/error.interceptor';
import { Project } from '../../core/models';
import { formatRelative } from '../../core/util/format';
import { ToastService } from '../../core/ui/toast.service';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { IconComponent } from '../../shared/icon/icon.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

type LoadState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    PageHeaderComponent,
    EmptyStateComponent,
    IconComponent,
    SpinnerComponent,
  ],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPage {
  private readonly api = inject(ProjectsApi);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly state = signal<LoadState>('loading');
  protected readonly projects = signal<Project[]>([]);
  protected readonly creating = signal(false);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly rel = formatRelative;

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(140)]],
    description: ['', [Validators.maxLength(1000)]],
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.api
      .list({ size: 60, sort: 'updatedAt,desc' })
      .pipe(catchError(() => of(null)))
      .subscribe((page) => {
        if (!page) {
          this.state.set('error');
          return;
        }
        this.projects.set(page.content);
        this.state.set('ready');
      });
  }

  protected toggleCreate(open: boolean): void {
    this.creating.set(open);
    this.formError.set(null);
    if (!open) this.form.reset();
  }

  protected create(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.formError.set(null);
    const { name, description } = this.form.getRawValue();

    this.api.create({ name: name.trim(), description: description.trim() || undefined }).subscribe({
      next: (project) => {
        this.toast.success('Proyecto creado', project.name);
        this.router.navigate(['/app/projects', project.id]);
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.formError.set(
          fieldErrorText(err) ?? readApiError(err)?.message ?? 'No se pudo crear el proyecto.',
        );
      },
    });
  }

  protected open(project: Project): void {
    this.router.navigate(['/app/projects', project.id]);
  }
}
