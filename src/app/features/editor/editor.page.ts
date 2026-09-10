import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription as RxSubscription, catchError, forkJoin, of, switchMap, timer } from 'rxjs';
import { ProjectsApi } from '../../core/api/projects-api.service';
import { RendersApi } from '../../core/api/renders-api.service';
import { VersionsApi } from '../../core/api/versions-api.service';
import { readApiError } from '../../core/auth/error.interceptor';
import { EditVersion, Project, ProjectGraph, RenderJob, isTerminalRender } from '../../core/models';
import { ToastService } from '../../core/ui/toast.service';
import { formatBytes, formatDate, formatDuration } from '../../core/util/format';
import { IconComponent } from '../../shared/icon/icon.component';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';
import { WaveformComponent } from '../../shared/waveform/waveform.component';

interface RenderForm {
  format: string;
  bitrateKbps: number;
  includeStems: boolean;
}

@Component({
  selector: 'app-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, IconComponent, SpinnerComponent, WaveformComponent],
  templateUrl: './editor.page.html',
  styleUrl: './editor.page.scss',
})
export class EditorPage {
  /** Bound from the route param via `withComponentInputBinding`. */
  readonly projectId = input.required<string>();

  private readonly projectsApi = inject(ProjectsApi);
  private readonly versionsApi = inject(VersionsApi);
  private readonly rendersApi = inject(RendersApi);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly project = signal<Project | null>(null);
  protected readonly graph = signal<ProjectGraph | null>(null);
  protected readonly versions = signal<EditVersion[]>([]);
  protected readonly renders = signal<RenderJob[]>([]);

  protected readonly savingVersion = signal(false);
  protected readonly showExport = signal(false);
  protected readonly enqueuing = signal(false);
  protected readonly restoringId = signal<string | null>(null);

  protected readonly form = signal<RenderForm>({ format: 'mp3', bitrateKbps: 256, includeStems: false });
  protected readonly bytes = formatBytes;
  protected readonly dur = formatDuration;
  protected readonly date = formatDate;

  private pollSub: RxSubscription | null = null;

  /** Longest timeline end across all clips, for laying blocks out proportionally. */
  protected readonly totalDuration = computed(() => {
    const g = this.graph();
    if (!g) return 0;
    let max = 0;
    for (const t of g.tracks) {
      for (const c of t.clips) max = Math.max(max, c.timelineStartSeconds + c.durationSeconds);
    }
    return Math.max(max, 1);
  });

  protected readonly ruler = computed(() => {
    const total = this.totalDuration();
    const step = niceStep(total / 8);
    const marks: number[] = [];
    for (let t = 0; t <= total + 0.001; t += step) marks.push(Math.round(t));
    return marks;
  });

  constructor() {
    // React to the route param (also covers navigating between projects).
    timer(0)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());
  }

  protected load(): void {
    const id = this.projectId();
    this.state.set('loading');
    forkJoin({
      project: this.projectsApi.open(id).pipe(catchError(() => of(null))),
      graph: this.projectsApi.graph(id).pipe(catchError(() => of(null))),
      versions: this.versionsApi.list(id, { size: 20 }).pipe(catchError(() => of(null))),
      renders: this.rendersApi.list(id, { size: 20 }).pipe(catchError(() => of(null))),
    }).subscribe(({ project, graph, versions, renders }) => {
      if (!project || !graph) {
        this.state.set('error');
        return;
      }
      this.project.set(project);
      this.graph.set(graph);
      this.versions.set(versions?.content ?? []);
      this.renders.set(renders?.content ?? []);
      this.state.set('ready');
      this.syncPolling();
    });
  }

  protected clipStyle(startSeconds: number, durationSeconds: number): Record<string, string> {
    const total = this.totalDuration();
    return {
      left: `${(startSeconds / total) * 100}%`,
      width: `${Math.max(2, (durationSeconds / total) * 100)}%`,
    };
  }

  protected saveVersion(): void {
    if (this.savingVersion()) return;
    const label = prompt('Etiqueta para esta versión (opcional):') ?? '';
    this.savingVersion.set(true);
    this.versionsApi.snapshot(this.projectId(), { label: label.trim() || undefined }).subscribe({
      next: (version) => {
        this.savingVersion.set(false);
        this.versions.update((list) => [version, ...list]);
        this.toast.success('Versión guardada', `Revisión ${version.revision}`);
      },
      error: (err: unknown) => {
        this.savingVersion.set(false);
        this.toast.error('No se pudo guardar la versión', readApiError(err)?.message ?? undefined);
      },
    });
  }

  protected restore(version: EditVersion): void {
    if (!confirm(`¿Restaurar la revisión ${version.revision}? Se guarda una copia del estado actual antes.`)) return;
    this.restoringId.set(version.id);
    this.versionsApi.restore(this.projectId(), version.id).subscribe({
      next: () => {
        this.restoringId.set(null);
        this.toast.success('Arreglo restaurado', `Revisión ${version.revision}`);
        this.load();
      },
      error: (err: unknown) => {
        this.restoringId.set(null);
        this.toast.error('No se pudo restaurar', readApiError(err)?.message ?? undefined);
      },
    });
  }

  protected updateForm<K extends keyof RenderForm>(key: K, value: RenderForm[K]): void {
    this.form.update((f) => ({ ...f, [key]: value }) as RenderForm);
  }

  protected enqueueRender(): void {
    if (this.enqueuing()) return;
    this.enqueuing.set(true);
    const f = this.form();
    this.rendersApi
      .enqueue(this.projectId(), {
        format: f.format,
        bitrateKbps: f.bitrateKbps,
        includeStems: f.includeStems,
      })
      .subscribe({
        next: (job) => {
          this.enqueuing.set(false);
          this.showExport.set(false);
          this.renders.update((list) => [job, ...list]);
          this.toast.success('Export en cola', `Job ${job.id.slice(0, 8)}`);
          this.syncPolling();
        },
        error: (err: unknown) => {
          this.enqueuing.set(false);
          this.toast.error('No se pudo encolar el export', readApiError(err)?.message ?? undefined);
        },
      });
  }

  protected cancelRender(job: RenderJob): void {
    this.rendersApi.cancel(this.projectId(), job.id).subscribe({
      next: (updated) => this.replaceJob(updated),
      error: (err: unknown) => this.toast.error('No se pudo cancelar', readApiError(err)?.message ?? undefined),
    });
  }

  protected download(job: RenderJob, stems: boolean): void {
    const call = stems
      ? this.rendersApi.stemsUrl(this.projectId(), job.id)
      : this.rendersApi.downloadUrl(this.projectId(), job.id);
    call.subscribe({
      next: ({ url }) => window.open(url, '_blank', 'noopener'),
      error: (err: unknown) => this.toast.error('No hay descarga disponible', readApiError(err)?.message ?? undefined),
    });
  }

  protected renderChip(status: RenderJob['status']): string {
    return status === 'SUCCEEDED'
      ? 'chip--ok'
      : status === 'FAILED'
        ? 'chip--danger'
        : status === 'RUNNING'
          ? 'chip--accent'
          : status === 'CANCELED'
            ? ''
            : 'chip--warn';
  }

  private replaceJob(updated: RenderJob): void {
    this.renders.update((list) => list.map((j) => (j.id === updated.id ? updated : j)));
    this.syncPolling();
  }

  /** Poll the render list while any job is still running or queued. */
  private syncPolling(): void {
    const hasActive = this.renders().some((j) => !isTerminalRender(j.status));
    if (hasActive && !this.pollSub) {
      this.pollSub = timer(4000, 4000)
        .pipe(
          switchMap(() => this.rendersApi.list(this.projectId(), { size: 20 }).pipe(catchError(() => of(null)))),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe((page) => {
          if (page) this.renders.set(page.content);
          if (!this.renders().some((j) => !isTerminalRender(j.status))) {
            this.pollSub?.unsubscribe();
            this.pollSub = null;
          }
        });
    }
  }
}

/** Round a raw axis step up to 1/2/5 × 10ⁿ so the ruler reads cleanly. */
function niceStep(raw: number): number {
  if (raw <= 0) return 1;
  const pow = Math.pow(10, Math.floor(Math.log10(raw)));
  const norm = raw / pow;
  const step = norm <= 1 ? 1 : norm <= 2 ? 2 : norm <= 5 ? 5 : 10;
  return step * pow;
}
