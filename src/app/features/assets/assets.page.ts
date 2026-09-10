import { DecimalPipe } from '@angular/common';
import { HttpEventType } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap, takeWhile, timer } from 'rxjs';
import { AssetsApi } from '../../core/api/assets-api.service';
import { readApiError } from '../../core/auth/error.interceptor';
import { AudioAsset } from '../../core/models';
import { ToastService } from '../../core/ui/toast.service';
import { formatBytes, formatDuration, formatRelative } from '../../core/util/format';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { IconComponent } from '../../shared/icon/icon.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

interface UploadRow {
  id: string;
  filename: string;
  size: number;
  progress: number;
  phase: 'uploading' | 'analyzing' | 'error';
  error?: string;
}

type LoadState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'app-assets',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, PageHeaderComponent, EmptyStateComponent, IconComponent, SpinnerComponent],
  templateUrl: './assets.page.html',
  styleUrl: './assets.page.scss',
})
export class AssetsPage {
  private readonly api = inject(AssetsApi);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly state = signal<LoadState>('loading');
  protected readonly assets = signal<AudioAsset[]>([]);
  protected readonly uploads = signal<UploadRow[]>([]);
  protected readonly busyId = signal<string | null>(null);

  protected readonly bytes = formatBytes;
  protected readonly dur = formatDuration;
  protected readonly rel = formatRelative;

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.api
      .list({ size: 60, sort: 'createdAt,desc' })
      .pipe(catchError(() => of(null)))
      .subscribe((page) => {
        if (!page) {
          this.state.set('error');
          return;
        }
        this.assets.set(page.content);
        this.state.set('ready');
      });
  }

  protected onPick(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    for (const file of files) this.startUpload(file);
  }

  private patchUpload(id: string, patch: Partial<UploadRow>): void {
    this.uploads.update((rows) => rows.map((r) => (r.id === id ? { ...r, ...patch } : r)));
  }

  private dropUpload(id: string): void {
    this.uploads.update((rows) => rows.filter((r) => r.id !== id));
  }

  private startUpload(file: File): void {
    const rowId = `${file.name}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
    this.uploads.update((rows) => [
      { id: rowId, filename: file.name, size: file.size, progress: 0, phase: 'uploading' },
      ...rows,
    ]);

    const contentType = file.type || 'application/octet-stream';
    this.api
      .initiateUpload({ filename: file.name, contentType, sizeBytes: file.size })
      .pipe(
        switchMap((reserved) =>
          this.api.putToStorage(reserved.upload, file).pipe(
            switchMap((event) => {
              if (event.type === HttpEventType.UploadProgress && event.total) {
                this.patchUpload(rowId, { progress: Math.round((event.loaded / event.total) * 100) });
                return of(null);
              }
              if (event.type === HttpEventType.Response) {
                this.patchUpload(rowId, { progress: 100, phase: 'analyzing' });
                return this.api.completeUpload(reserved.assetId).pipe(switchMap(() => this.poll(reserved.assetId)));
              }
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (asset) => {
          if (!asset) return;
          this.dropUpload(rowId);
          this.assets.update((list) => [asset, ...list.filter((a) => a.id !== asset.id)]);
          if (asset.status === 'FAILED') {
            this.toast.error('El análisis falló', asset.failureReason ?? file.name);
          } else {
            this.toast.success('Asset listo', asset.originalFilename);
          }
        },
        error: (err: unknown) => {
          this.patchUpload(rowId, {
            phase: 'error',
            error: readApiError(err)?.message ?? 'La subida falló.',
          });
        },
      });
  }

  /** Poll a single asset until it leaves the PROCESSING state. */
  private poll(assetId: string) {
    return timer(0, 2200).pipe(
      switchMap(() => this.api.get(assetId)),
      takeWhile((a) => a.status === 'PENDING_UPLOAD' || a.status === 'PROCESSING', true),
    );
  }

  protected download(asset: AudioAsset): void {
    this.busyId.set(asset.id);
    this.api.downloadUrl(asset.id).subscribe({
      next: ({ url }) => {
        this.busyId.set(null);
        window.open(url, '_blank', 'noopener');
      },
      error: () => this.busyId.set(null),
    });
  }

  protected remove(asset: AudioAsset): void {
    if (!confirm(`¿Eliminar "${asset.originalFilename}"? No se puede si algún proyecto lo usa.`)) return;
    this.busyId.set(asset.id);
    this.api.delete(asset.id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.assets.update((list) => list.filter((a) => a.id !== asset.id));
        this.toast.success('Asset eliminado');
      },
      error: (err: unknown) => {
        this.busyId.set(null);
        this.toast.error('No se pudo eliminar', readApiError(err)?.message ?? undefined);
      },
    });
  }

  protected statusChip(status: AudioAsset['status']): { cls: string; label: string } {
    switch (status) {
      case 'READY':
        return { cls: 'chip--ok', label: 'Listo' };
      case 'PROCESSING':
        return { cls: 'chip--warn', label: 'Analizando' };
      case 'PENDING_UPLOAD':
        return { cls: '', label: 'Pendiente' };
      case 'FAILED':
        return { cls: 'chip--danger', label: 'Error' };
    }
  }
}
