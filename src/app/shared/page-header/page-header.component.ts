import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="ph">
      <div class="ph__text">
        <h1>{{ heading() }}</h1>
        @if (sub()) {
          <p>{{ sub() }}</p>
        }
      </div>
      <div class="ph__actions"><ng-content /></div>
    </header>
  `,
  styles: [
    `
      .ph {
        display: flex;
        flex-wrap: wrap;
        align-items: flex-end;
        justify-content: space-between;
        gap: 1rem;
        margin-bottom: clamp(1.5rem, 4vw, 2.5rem);
      }
      h1 {
        font-size: var(--step-3);
      }
      p {
        margin-top: 0.4rem;
        color: var(--text-dim);
        font-size: var(--step--1);
        max-width: 60ch;
      }
      .ph__actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.6rem;
      }
      .ph__actions:empty {
        display: none;
      }
    `,
  ],
})
export class PageHeaderComponent {
  readonly heading = input.required<string>();
  readonly sub = input('');
}
