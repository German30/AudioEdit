import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { catchError, forkJoin, of } from 'rxjs';
import { PlansApi } from '../../core/api/plans-api.service';
import { SubscriptionApi } from '../../core/api/subscription-api.service';
import { readApiError } from '../../core/auth/error.interceptor';
import { Plan, PlanTier, Subscription } from '../../core/models';
import { ToastService } from '../../core/ui/toast.service';
import { formatBytes, formatDate, formatPrice } from '../../core/util/format';
import { IconComponent } from '../../shared/icon/icon.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';
import { UsageMeterComponent } from '../../shared/usage-meter/usage-meter.component';

@Component({
  selector: 'app-subscription',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageHeaderComponent, UsageMeterComponent, IconComponent, SpinnerComponent],
  templateUrl: './subscription.page.html',
  styleUrl: './subscription.page.scss',
})
export class SubscriptionPage {
  private readonly subApi = inject(SubscriptionApi);
  private readonly plansApi = inject(PlansApi);
  private readonly toast = inject(ToastService);

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly sub = signal<Subscription | null>(null);
  protected readonly plans = signal<Plan[]>([]);
  protected readonly pendingTier = signal<PlanTier | null>(null);
  protected readonly canceling = signal(false);

  protected readonly date = formatDate;

  protected readonly renderMinutes = computed(() => {
    const u = this.sub()?.usage;
    if (!u) return { used: 0, limit: 0, text: '' };
    const limit = u.renderSecondsLimit < 0 ? -1 : Math.round(u.renderSecondsLimit / 60);
    const used = Math.round(u.renderSecondsUsed / 60);
    return { used, limit, text: `${used} / ${limit < 0 ? '∞' : limit} min` };
  });

  protected readonly storage = computed(() => {
    const u = this.sub()?.usage;
    if (!u) return { used: 0, limit: 0, text: '' };
    return {
      used: u.storageBytesUsed,
      limit: u.storageBytesLimit,
      text: `${formatBytes(u.storageBytesUsed)} / ${u.storageBytesLimit < 0 ? '∞' : formatBytes(u.storageBytesLimit)}`,
    };
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    forkJoin({
      sub: this.subApi.current().pipe(catchError(() => of(null))),
      plans: this.plansApi.list().pipe(catchError(() => of([] as Plan[]))),
    }).subscribe(({ sub, plans }) => {
      if (!sub) {
        this.state.set('error');
        return;
      }
      this.sub.set(sub);
      this.plans.set([...plans].sort((a, b) => a.priceCents - b.priceCents));
      this.state.set('ready');
    });
  }

  protected price(plan: Plan): string {
    return formatPrice(plan.priceCents, plan.currency);
  }

  protected isCurrent(tier: PlanTier): boolean {
    return this.sub()?.tier === tier;
  }

  protected statusLabel(status: Subscription['status']): string {
    return (
      { TRIALING: 'En prueba', ACTIVE: 'Activa', PAST_DUE: 'Pago pendiente', CANCELED: 'Cancelada' }[status] ??
      status
    );
  }

  protected statusClass(status: Subscription['status']): string {
    return status === 'ACTIVE' || status === 'TRIALING'
      ? 'chip--ok'
      : status === 'PAST_DUE'
        ? 'chip--warn'
        : 'chip--danger';
  }

  protected changePlan(tier: PlanTier): void {
    if (this.isCurrent(tier) || this.pendingTier()) return;
    this.pendingTier.set(tier);
    this.subApi.change({ tier }).subscribe({
      next: (updated) => {
        this.pendingTier.set(null);
        this.sub.set(updated);
        this.toast.success('Plan actualizado', `Ahora estás en ${updated.tier}.`);
      },
      error: (err: unknown) => {
        this.pendingTier.set(null);
        this.toast.error('No se pudo cambiar de plan', readApiError(err)?.message ?? undefined);
      },
    });
  }

  protected cancel(): void {
    if (!confirm('El plan seguirá activo hasta el final del periodo y luego pasará a Free. ¿Continuar?')) return;
    this.canceling.set(true);
    this.subApi.cancel({ immediately: false }).subscribe({
      next: (updated) => {
        this.canceling.set(false);
        this.sub.set(updated);
        this.toast.info('Cancelación programada', 'Tu plan no se renovará.');
      },
      error: (err: unknown) => {
        this.canceling.set(false);
        this.toast.error('No se pudo cancelar', readApiError(err)?.message ?? undefined);
      },
    });
  }

  protected reactivate(): void {
    this.canceling.set(true);
    this.subApi.reactivate().subscribe({
      next: (updated) => {
        this.canceling.set(false);
        this.sub.set(updated);
        this.toast.success('Plan reactivado');
      },
      error: (err: unknown) => {
        this.canceling.set(false);
        this.toast.error('No se pudo reactivar', readApiError(err)?.message ?? undefined);
      },
    });
  }

  protected planLines(plan: Plan): { label: string; value: string }[] {
    const e = plan.entitlements;
    return [
      { label: 'Proyectos', value: e.maxProjects < 0 ? 'Ilimitado' : String(e.maxProjects) },
      { label: 'Render / mes', value: e.monthlyRenderMinutes < 0 ? 'Ilimitado' : `${e.monthlyRenderMinutes} min` },
      { label: 'Almacenamiento', value: formatBytes(e.maxStorageBytes, 0) },
      { label: 'Formatos', value: e.allowedExportFormats.map((f) => f.toUpperCase()).join(' · ') },
      { label: 'Stems', value: e.allowStemExport ? 'Sí' : '—' },
      { label: 'Bitrate máx.', value: `${e.maxExportBitrateKbps} kbps` },
    ];
  }
}
