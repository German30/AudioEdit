import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from '../../core/ui/toast.service';
import { IconComponent, IconName } from '../icon/icon.component';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stack" aria-live="polite" aria-atomic="false">
      @for (t of toasts.toasts(); track t.id) {
        <div class="toast" [class]="t.kind" role="status">
          <app-icon [name]="glyph(t.kind)" [size]="16" />
          <div class="body">
            <p class="title">{{ t.title }}</p>
            @if (t.detail) {
              <p class="detail">{{ t.detail }}</p>
            }
          </div>
          <button type="button" class="dismiss" (click)="toasts.dismiss(t.id)" aria-label="Cerrar aviso">
            <app-icon name="close" [size]="14" />
          </button>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .stack {
        position: fixed;
        right: clamp(12px, 3vw, 24px);
        bottom: clamp(12px, 3vw, 24px);
        z-index: 1000;
        display: flex;
        flex-direction: column;
        gap: 0.6rem;
        width: min(360px, calc(100vw - 24px));
      }
      .toast {
        display: grid;
        grid-template-columns: auto 1fr auto;
        gap: 0.7rem;
        align-items: start;
        padding: 0.8rem 0.9rem;
        border-radius: var(--r-sm);
        background: var(--surface-2);
        border: 1px solid var(--line-strong);
        box-shadow: var(--shadow-lg);
        animation: toast-in 0.3s var(--ease-out);
      }
      @keyframes toast-in {
        from {
          opacity: 0;
          transform: translateY(8px) scale(0.98);
        }
      }
      .toast.success {
        border-color: rgba(79, 216, 164, 0.4);
      }
      .toast.success app-icon {
        color: var(--ok);
      }
      .toast.error {
        border-color: rgba(255, 107, 107, 0.42);
      }
      .toast.error app-icon {
        color: var(--danger);
      }
      .toast.info app-icon {
        color: var(--accent);
      }
      .title {
        font-weight: 600;
        font-size: var(--step--1);
      }
      .detail {
        color: var(--text-dim);
        font-size: var(--step--1);
        margin-top: 0.15rem;
      }
      .dismiss {
        color: var(--text-mute);
        padding: 2px;
        border-radius: 4px;
      }
      .dismiss:hover {
        color: var(--text);
      }
    `,
  ],
})
export class ToastHostComponent {
  protected readonly toasts = inject(ToastService);

  protected glyph(kind: 'info' | 'success' | 'error'): IconName {
    return kind === 'success' ? 'check' : kind === 'error' ? 'alert' : 'spark';
  }
}
