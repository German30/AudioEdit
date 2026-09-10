import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * A labelled consumption bar: "Render  12 / 600 min" with a fill that shifts to
 * amber past 75% and red past 90%. An unlimited limit (`-1`) shows no track.
 */
@Component({
  selector: 'app-usage-meter',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="head">
      <span class="label">{{ label() }}</span>
      <span class="value tnum">{{ display() }}</span>
    </div>
    @if (unlimited()) {
      <div class="infinite">Sin límite en este plan</div>
    } @else {
      <div class="track" role="progressbar" [attr.aria-valuenow]="pct()" aria-valuemin="0" aria-valuemax="100">
        <div class="fill" [class.warn]="pct() >= 75" [class.crit]="pct() >= 90" [style.width.%]="pct()"></div>
      </div>
    }
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .head {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        gap: 1rem;
        margin-bottom: 0.5rem;
      }
      .label {
        font-size: var(--step--1);
        color: var(--text-dim);
        font-weight: 500;
      }
      .value {
        font-family: var(--font-mono);
        font-size: var(--step--1);
        color: var(--text);
      }
      .track {
        height: 7px;
        border-radius: 999px;
        background: var(--surface-3);
        overflow: hidden;
      }
      .fill {
        height: 100%;
        border-radius: 999px;
        background: var(--accent);
        box-shadow: 0 0 12px var(--accent-glow);
        transition: width 0.5s var(--ease-out);
      }
      .fill.warn {
        background: var(--warn);
        box-shadow: 0 0 12px var(--warn-soft);
      }
      .fill.crit {
        background: var(--danger);
        box-shadow: 0 0 12px var(--danger-soft);
      }
      .infinite {
        font-size: var(--step--1);
        color: var(--text-mute);
        padding: 0.25rem 0;
      }
    `,
  ],
})
export class UsageMeterComponent {
  readonly label = input.required<string>();
  readonly used = input.required<number>();
  readonly limit = input.required<number>();
  /** Pre-formatted "used / limit" text; falls back to raw numbers. */
  readonly valueText = input<string>('');

  protected readonly display = computed(
    () => this.valueText() || `${Math.round(this.used())} / ${this.limit() < 0 ? '∞' : Math.round(this.limit())}`,
  );
  protected readonly unlimited = computed(() => this.limit() < 0);
  protected readonly pct = computed(() => {
    const l = this.limit();
    if (l <= 0) return 0;
    return Math.max(0, Math.min(100, Math.round((this.used() / l) * 100)));
  });
}
