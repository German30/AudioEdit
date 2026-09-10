import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IconComponent, IconName } from '../icon/icon.component';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="wrap">
      <span class="glyph"><app-icon [name]="icon()" [size]="24" /></span>
      <h3>{{ heading() }}</h3>
      <p>{{ message() }}</p>
      <div class="slot"><ng-content /></div>
    </div>
  `,
  styles: [
    `
      .wrap {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        gap: 0.6rem;
        padding: clamp(2.5rem, 6vw, 4.5rem) 1.5rem;
      }
      .glyph {
        display: grid;
        place-items: center;
        width: 52px;
        height: 52px;
        border-radius: var(--r-md);
        background: var(--surface-2);
        border: 1px solid var(--line);
        color: var(--accent);
        margin-bottom: 0.3rem;
      }
      h3 {
        font-size: var(--step-1);
      }
      p {
        color: var(--text-dim);
        max-width: 42ch;
        font-size: var(--step--1);
      }
      .slot:not(:empty) {
        margin-top: 0.8rem;
      }
    `,
  ],
})
export class EmptyStateComponent {
  readonly icon = input<IconName>('spark');
  readonly heading = input.required<string>();
  readonly message = input('');
}
