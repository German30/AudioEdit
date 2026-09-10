import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Deterministic 0..1 pseudo-noise so the shape is stable between renders. */
function shape(count: number, seed: number): number[] {
  const out: number[] = [];
  let s = seed || 1;
  for (let i = 0; i < count; i++) {
    s = (s * 1664525 + 1013904223) % 4294967296;
    const rnd = s / 4294967296;
    // envelope: swell in the middle, taper at the edges
    const env = Math.sin((i / (count - 1)) * Math.PI);
    const detail = 0.35 + 0.65 * rnd;
    out.push(Math.max(0.08, env * detail));
  }
  return out;
}

/**
 * A stylised audio waveform rendered as SVG bars. `animated` gives it a gentle
 * "live meter" motion for hero use; static elsewhere.
 */
@Component({
  selector: 'app-waveform',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.viewBox]="'0 0 ' + width() + ' 100'"
      preserveAspectRatio="none"
      class="wave"
      [class.animated]="animated()"
      aria-hidden="true"
    >
      @for (h of bars(); track $index) {
        <rect
          [attr.x]="$index * (gap() + barWidth())"
          [attr.y]="50 - h * 48"
          [attr.width]="barWidth()"
          [attr.height]="h * 96"
          [attr.rx]="barWidth() / 2"
          [style.animation-delay.ms]="$index * 55"
        />
      }
    </svg>
  `,
  styles: [
    `
      :host {
        display: block;
        width: 100%;
      }
      .wave {
        display: block;
        width: 100%;
        height: 100%;
        overflow: visible;
      }
      rect {
        fill: var(--accent);
        transform-box: fill-box;
        transform-origin: center;
      }
      .animated rect {
        animation: pulse 1.8s var(--ease-out) infinite alternate;
      }
      @keyframes pulse {
        from {
          transform: scaleY(0.55);
          opacity: 0.65;
        }
        to {
          transform: scaleY(1);
          opacity: 1;
        }
      }
      @media (prefers-reduced-motion: reduce) {
        .animated rect {
          animation: none;
        }
      }
    `,
  ],
})
export class WaveformComponent {
  readonly count = input(64);
  readonly seed = input(7);
  readonly animated = input(false);
  readonly gap = input(3);
  readonly barWidth = input(4);

  protected readonly bars = computed(() => shape(this.count(), this.seed()));
  protected readonly width = computed(() => this.count() * (this.gap() + this.barWidth()));
}
