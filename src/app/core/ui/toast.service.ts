import { Injectable, signal } from '@angular/core';

export type ToastKind = 'info' | 'success' | 'error';

export interface Toast {
  id: number;
  kind: ToastKind;
  title: string;
  detail?: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private seq = 0;
  readonly toasts = signal<Toast[]>([]);

  show(kind: ToastKind, title: string, detail?: string, ttlMs = 5000): void {
    const id = ++this.seq;
    this.toasts.update((list) => [...list, { id, kind, title, detail }]);
    if (ttlMs > 0) setTimeout(() => this.dismiss(id), ttlMs);
  }

  success(title: string, detail?: string): void {
    this.show('success', title, detail);
  }

  error(title: string, detail?: string): void {
    this.show('error', title, detail, 7000);
  }

  info(title: string, detail?: string): void {
    this.show('info', title, detail);
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }
}
